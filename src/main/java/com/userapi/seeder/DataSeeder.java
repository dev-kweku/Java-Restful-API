package com.userapi.seeder;

import com.userapi.config.DatabaseConfig;
import javax.sql.DataSource;
import java.sql.*;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

public class DataSeeder {
    private static final int BATCH_SIZE   = 5_000;
    private static final int THREAD_COUNT = 8;

    private final AtomicLong totalInserted = new AtomicLong(0);

    private static final String[] FIRST_NAMES = {
            "James","Mary","John","Patricia","Robert","Jennifer","Michael","Linda",
            "William","Barbara","David","Elizabeth","Richard","Susan","Joseph","Jessica",
            "Kofi","Ama","Kwame","Akosua","Yaw","Abena","Kweku","Adwoa"
    };
    private static final String[] LAST_NAMES = {
            "Smith","Johnson","Williams","Brown","Jones","Garcia","Miller","Davis",
            "Mensah","Asante","Osei","Boateng","Agyei","Owusu","Amoah","Darko"
    };
    private static final String[] CITIES = {
            "Accra","Kumasi","New York","London","Lagos","Nairobi","Paris","Berlin",
            "Tokyo","Sydney","Toronto","Dubai","Singapore","Cape Town"
    };
    private static final String[] COUNTRIES = {
            "Ghana","USA","UK","Nigeria","Kenya","France","Germany",
            "Japan","Australia","Canada","UAE","Singapore","Brazil","South Africa"
    };
    private static final String[] GENDERS = {"Male", "Female"};

    public DataSeeder() {}

    public void seed(int total) throws Exception {


        System.out.println("Testing DB connection...");
        System.out.flush();
        DataSource ds = DatabaseConfig.getDataSource();
        try (Connection test = ds.getConnection()) {
            System.out.println("DB connection OK — " + test.getMetaData().getURL());
            System.out.flush();
        } catch (SQLException e) {
            System.err.println("Cannot connect to DB: " + e.getMessage());
            System.err.println("Check your .env file has DB_URL, DB_USER, DB_PASSWORD set correctly.");
            System.err.flush();
            throw e;
        }

        totalInserted.set(0);

        System.out.println("======SEEDER START======");
        System.out.printf("Target  : %,d records%n",     total);
        System.out.printf("Threads : %d%n",              THREAD_COUNT);
        System.out.printf("Batch   : %,d rows/commit%n", BATCH_SIZE);
        System.out.flush();

        long start     = System.currentTimeMillis();
        int  base      = total / THREAD_COUNT;
        int  remainder = total % THREAD_COUNT;

        ExecutorService executor    = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch  latch       = new CountDownLatch(THREAD_COUNT);
        AtomicLong      failedCount = new AtomicLong(0);

        for (int t = 0; t < THREAD_COUNT; t++) {

            final int count    = (t == THREAD_COUNT - 1) ? base + remainder : base;
            final int threadId = t;

            executor.submit(() -> {
                try {

                    insertBatch(ds, count, threadId);
                } catch (Exception e) {
                    failedCount.incrementAndGet();
                    System.err.printf("[Thread-%d] FAILED: %s%n", threadId, e.getMessage());
                    e.printStackTrace();
                    System.err.flush();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        long   elapsed = System.currentTimeMillis() - start;
        long   dbCount = verifyCount(ds);
        double rps     = dbCount / Math.max(elapsed / 1000.0, 0.001);

        System.out.println("======SEEDER DONE======");
        System.out.printf("Inserted : %,d records%n",    totalInserted.get());
        System.out.printf("Verified : %,d in DB%n",      dbCount);
        System.out.printf("Failed   : %d threads%n",     failedCount.get());
        System.out.printf("Time     : %.2f seconds%n",   elapsed / 1000.0);
        System.out.printf("Speed    : %.0f records/s%n", rps);

        if (dbCount < total) {
            System.err.printf("WARNING: Only %,d of %,d records inserted!%n", dbCount, total);
            System.err.flush();
        } else {
            System.out.printf("✓ Verified: exactly %,d records in DB%n", dbCount);
        }
        System.out.flush();
    }


    private void insertBatch(DataSource ds, int count, int threadId) throws Exception {
        var rng = ThreadLocalRandom.current();

        String sql = """
            INSERT INTO users (first_name, last_name, email, phone, city, country, age, gender)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (email) DO NOTHING
            """;

        long threadInserted = 0;

        try (Connection conn = ds.getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                for (int i = 0; i < count; i++) {
                    String firstName = FIRST_NAMES[rng.nextInt(FIRST_NAMES.length)];
                    String lastName  = LAST_NAMES [rng.nextInt(LAST_NAMES.length)];
                    String email     = firstName.toLowerCase() + "."
                            + lastName.toLowerCase() + "."
                            + UUID.randomUUID().toString().replace("-", "").substring(0, 10)
                            + "@example.com";

                    ps.setString(1, firstName);
                    ps.setString(2, lastName);
                    ps.setString(3, email);
                    ps.setString(4, String.format("+233%09d", rng.nextInt(1_000_000_000)));
                    ps.setString(5, CITIES   [rng.nextInt(CITIES.length)]);
                    ps.setString(6, COUNTRIES[rng.nextInt(COUNTRIES.length)]);
                    ps.setInt   (7, 18 + rng.nextInt(62));
                    ps.setString(8, GENDERS  [rng.nextInt(GENDERS.length)]);
                    ps.addBatch();

                    if ((i + 1) % BATCH_SIZE == 0) {
                        int[] results    = ps.executeBatch();
                        long  batchCount = countInserted(results);
                        conn.commit();

                        threadInserted      += batchCount;
                        long globalSoFar     = totalInserted.addAndGet(batchCount);
                        System.out.printf("[Thread-%02d] %,8d / %,d | global %,d%n",
                                threadId, i + 1, count, globalSoFar);
                        System.out.flush();
                    }
                }


                int[] results   = ps.executeBatch();
                long  lastCount = countInserted(results);
                conn.commit();
                threadInserted += lastCount;
                totalInserted.addAndGet(lastCount);

                System.out.printf("[Thread-%02d] DONE — inserted %,d records%n",
                        threadId, threadInserted);
                System.out.flush();

            } catch (Exception e) {
                conn.rollback();
                System.err.printf("[Thread-%d] Rolled back: %s%n", threadId, e.getMessage());
                System.err.flush();
                throw e;
            }
        }
    }

    private long countInserted(int[] results) {
        long count = 0;
        for (int r : results) {
            if      (r == Statement.SUCCESS_NO_INFO) count++;
            else if (r >= 0)                         count += r;
        }
        return count;
    }


    private long verifyCount(DataSource ds) throws SQLException {
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM users")) {
            ResultSet rs = ps.executeQuery();
            long count   = rs.next() ? rs.getLong(1) : 0;
            System.out.printf("DB verification: %,d rows in users table%n", count);
            System.out.flush();
            return count;
        }
    }
}