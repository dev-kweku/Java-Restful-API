package com.userapi.seeder;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.sql.PreparedStatement;
import java.util.concurrent.ThreadLocalRandom;

public class DataSeeder {
    private static final int BATCH_SIZE=5_000;
    private static final int THREAD_COUNT=8;

    private final DataSource dataSource;
    private final Random random=new Random();


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

    public void seed(int total) throws Exception{
        System.out.println("Starting seeder :" +total + " records across " + THREAD_COUNT + " threads...");
        long start=System.currentTimeMillis();

        int perThread=total/THREAD_COUNT;
        ExecutorService executor= Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch latch=new CountDownLatch(THREAD_COUNT);

        for(int t=0;t<THREAD_COUNT;t++){
            final int threadId=t;
            executor.submit(()->{
                try{
                    insertBatch(perThread,threadId);
                }catch(Exception e){
                    e.printStackTrace();
                }finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        long elapsed=System.currentTimeMillis()-start;
        System.out.printf("Seeding done in %.2f seconds%n",elapsed/1000.0);
    }

    private void insertBatch(int count, int threadId) throws Exception {
        String sql= """
            INSERT INTO users (first_name,last_name,email,phone,city,country,age,gender) 
            VALUES (?,?,?,?,?,?,?,?) ON CONFLICT (email) DO NOTHING """;

        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false); // manual transaction for speed

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                for (int i = 0; i < count; i++) {
                    String firstName = FIRST_NAMES[random.nextInt(FIRST_NAMES.length)];
                    String lastName  = LAST_NAMES[random.nextInt(LAST_NAMES.length)];
                    String email     = firstName.toLowerCase() + "."
                            + lastName.toLowerCase() + "."
                            + UUID.randomUUID().toString().substring(0, 8)
                            + "@example.com";

                    ps.setString(1, firstName);
                    ps.setString(2, lastName);
                    ps.setString(3, email);
                    ps.setString(4, randomPhone((ThreadLocalRandom) random));
                    ps.setString(5, CITIES[random.nextInt(CITIES.length)]);
                    ps.setString(6, COUNTRIES[random.nextInt(COUNTRIES.length)]);
                    ps.setInt(7, 18 + random.nextInt(62));
                    ps.setString(8, GENDERS[random.nextInt(GENDERS.length)]);
                    ps.addBatch();

                    if ((i + 1) % BATCH_SIZE == 0) {
                        ps.executeBatch();
                        conn.commit();
                        System.out.printf("[Thread-%d] Inserted %d/%d%n", threadId, i + 1, count);
                    }
                }
                ps.executeBatch();
                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        }
    }

    private String randomPhone(ThreadLocalRandom random){
        return String.format("+233%09d",random.nextInt(1_000_000_000));
    }
}
