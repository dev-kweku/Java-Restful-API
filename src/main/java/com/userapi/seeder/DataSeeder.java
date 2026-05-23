package com.userapi.seeder;

import javax.sql.DataSource;
import java.sql.*;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

public class DataSeeder {
    private static final int BATCH_SIZE=5_000;
    private static final int THREAD_COUNT=8;

    private final DataSource dataSource;

    private final AtomicLong totalInserted=new AtomicLong(0);



    private static final String[] FIRST_NAMES={
            "James","Mary","John","Patricia","Robert","Jennifer","Michael","Linda",
            "William","Barbara","David","Elizabeth","Richard","Susan","Joseph","Jessica",
            "Kofi","Ama","Kwame","Akosua","Yaw","Abena","Kweku","Adwoa"
    };
    private static final String[] LAST_NAMES={
           "Smith","Johnson","Williams","Brown","Jones","Garcia","Miller","Davis",
            "Mensah","Asante","Osei","Boateng","Agyei","Owusu","Amoah","Darko"
    };
    private static final String[] CITIES={
            "Accra","Kumasi","New York","London","Lagos","Nairobi","Paris","Berlin",
            "Tokyo","Sydney","Toronto","Dubai","Singapore","São Paulo","Cape Town"
    };
    private static final String[] COUNTRIES={
            "Ghana","USA","UK","Nigeria","Kenya","France","Germany",
            "Japan","Australia","Canada","UAE","Singapore","Brazil","South Africa"
    };
    private static final String[] GENDERS={"Male","Female"};

    public DataSeeder(DataSource dataSource){
        this.dataSource=dataSource;
    }

//    public void seed(int total) throws Exception{
//        System.out.println("Starting seeder :" +total + " records across " + THREAD_COUNT + " threads...");
//        long start=System.currentTimeMillis();
//
//        int perThread=total/THREAD_COUNT;
//        ExecutorService executor= Executors.newFixedThreadPool(THREAD_COUNT);
//        CountDownLatch latch=new CountDownLatch(THREAD_COUNT);
//
//        for(int t=0;t<THREAD_COUNT;t++){
//            final int threadId=t;
//            executor.submit(()->{
//                try{
//                    insertBatch(perThread,threadId);
//                }catch(Exception e){
//                    e.printStackTrace();
//                }finally {
//                    latch.countDown();
//                }
//            });
//        }
//
//        latch.await();
//        executor.shutdown();
//
//        long elapsed=System.currentTimeMillis()-start;
//        System.out.printf("Seeding done in %.2f seconds%n",elapsed/1000.0);
//    }


    public void seed(int total) throws Exception{
        totalInserted.set(0);
        System.out.println("======SEEDER START======");
        System.out.printf("Target : %,d records%n",total);
        System.out.printf("Threads : %d%n",THREAD_COUNT);
        System.out.printf("Batch : %,d rows/commit%n", BATCH_SIZE);

        long start = System.currentTimeMillis();
        int base=total/THREAD_COUNT;
        int remainder=total % THREAD_COUNT;

        ExecutorService executor=Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch latch=new CountDownLatch(THREAD_COUNT);

        AtomicLong failedThreads=new AtomicLong(0);
        for(int t=0;t<THREAD_COUNT;t++){
            final int count=(t==THREAD_COUNT-1) ? base + remainder:base;
            final int threadId=t;

            executor.submit(()->{
                try{
                    insertBatch(count,threadId);
                }catch (Exception e){
                    failedThreads.incrementAndGet();
                    System.err.printf("[Thread-%d] FAILED  %s%n:",threadId,e.getMessage() );

                    e.printStackTrace();
                }finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        long elapsed=System.currentTimeMillis() - start;

        long dBCount=verifyCount();
        double rps=dBCount/Math.max(elapsed/1000.0,0.001);

        System.out.println("=======SEEDER DONE =======");
        System.out.printf("Inserted (counted) :%d%n ",dBCount);
        System.out.printf("DB verified count :%d%n",failedThreads.get());
        System.out.printf("Time         :%.2f seconds%n", elapsed/1000.0);
        System.out.printf("Speed         :%.0f records/second%n",rps );

        if(dBCount < total){
            System.err.printf("WARNING : Only %,d of %,d records inserted!%n",dBCount,total);

        }else {
            System.out.printf("Verified: exactly %,d records in DB%n",dBCount);
        }
    }

    private long verifyCount() throws SQLException{
        try(Connection c=dataSource.getConnection();PreparedStatement ps=c.prepareStatement("SELECT COUNT(*) FROM users")){
            ResultSet rs=ps.executeQuery();
            long count=rs.next() ? rs.getLong(1) : 0;
            System.out.printf("DB verification : %,d rows in users table%n",count);
            return count;
        }
    }

    private  long countInserted(int[] results){
        long count=0;
        for(int r:results){
            if(r== Statement.SUCCESS_NO_INFO) count++;
            else if(r>= 0) count += r;
        }
        return count;
    }

    private void insertBatch(int count, int threadId) throws Exception {

        var rng = ThreadLocalRandom.current();

        String sql = """
            INSERT INTO users (first_name, last_name, email, phone, city, country, age, gender)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (email) DO NOTHING
            """;

        long threadInserted = 0;

        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                for (int i = 0; i < count; i++) {
                    String firstName = FIRST_NAMES[rng.nextInt(FIRST_NAMES.length)];
                    String lastName  = LAST_NAMES[rng.nextInt(LAST_NAMES.length)];


                    String email = firstName.toLowerCase() + "."
                            + lastName.toLowerCase() + "."
                            + UUID.randomUUID().toString().replace("-", "").substring(0, 10)
                            + "@example.com";

                    ps.setString(1, firstName);
                    ps.setString(2, lastName);
                    ps.setString(3, email);
                    ps.setString(4, randomPhone(rng));
                    ps.setString(5, CITIES[rng.nextInt(CITIES.length)]);
                    ps.setString(6, COUNTRIES[rng.nextInt(COUNTRIES.length)]);
                    ps.setInt   (7, 18 + rng.nextInt(62));
                    ps.setString(8, GENDERS[rng.nextInt(GENDERS.length)]);
                    ps.addBatch();

                    if ((i + 1) % BATCH_SIZE == 0) {

                        int[] results   = ps.executeBatch();
                        long batchCount = countInserted(results);
                        conn.commit();

                        threadInserted         += batchCount;
                        long globalSoFar        = totalInserted.addAndGet(batchCount);
                        System.out.printf("[Thread-%02d] %,8d / %,d | global %,d%n",
                                threadId, i + 1, count, globalSoFar);
                    }
                }


                int[] results  = ps.executeBatch();
                long lastCount = countInserted(results);
                conn.commit();

                threadInserted += lastCount;
                totalInserted.addAndGet(lastCount);

                System.out.printf("[Thread-%02d] DONE — inserted %,d records%n",
                        threadId, threadInserted);

            } catch (Exception e) {
                conn.rollback();
                System.err.printf("[Thread-%d] Rolled back: %s%n", threadId, e.getMessage());
                throw e;
            }
        }
    }


    private String randomPhone(ThreadLocalRandom rng){
        return String.format("+233%09d",rng.nextInt(1_000_000_000));
    }
}
