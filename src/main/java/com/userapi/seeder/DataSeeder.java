package com.userapi.seeder;

import javax.sql.DataSource;
import java.util.Random;

public class DataSeeder {
    private static final int BATCH_SIZE=5_000;
    private static final int THREAD_COUNT=8;

    private final DataSource dataSource;
    private final Random random=new Random();


    private static final String[] FIRST_NAMES={};
    private static final String[] LAST_NAME={};
    private static final String[] CITIES={};
    private static final String[] COUNTRIES={};
    private static final String[] GENDERS={};

    public DataSeeder(DataSource dataSource){
        this.dataSource=dataSource;
    }

    public void seed(int total) throws Exception{

    }

    public void insertBatch() throws Exception{};

    private String randomPhone(){
        return String.format("+233%09d",random.nextInt(1_000_000_000));
    }
}
