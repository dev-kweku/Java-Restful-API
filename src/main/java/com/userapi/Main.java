package com.userapi;

import com.userapi.config.DatabaseConfig;
import com.userapi.controller.UserController;
import com.userapi.repository.UserRepository;
import com.userapi.seeder.DataSeeder;
import com.userapi.server.HttpServer;
import com.userapi.server.Router;
import com.userapi.service.UserService;

public class Main {
    public static void main(String[] args) throws Exception {
        System.setOut(new java.io.PrintStream(System.out, true));
        System.setErr(new java.io.PrintStream(System.err, true));

        boolean seedMode=args.length > 0 && args[0].equals("--seed");
//        var dataSource= DatabaseConfig.getDataSource();

        System.out.println("Connecting to database....");
        var dataSource=DatabaseConfig.getDataSource();
        System.out.println("Database connected OK");

        var userRepo=new UserRepository(dataSource);
        var userService=new UserService(userRepo);
        var userController=new UserController(userService);

//        if(args.length > 0 && args[0].equals("--seed")){
//            new DataSeeder(dataSource).seed(1_000_000);
//            System.out.println("Seeding complete. Existing.");
//            return;
//        }

        if(seedMode){
           new DataSeeder().seed(1_000_000);
            System.out.println("Seeding complete. Existing.");
            return;
        }

        var router=new Router();
        router.get("/api/users", userController::getAll);
        router.get("/api/users/{id}", userController::getById);
        router.post("/api/users", userController::create);
        router.put("/api/users/{id}", userController::update);
        router.delete("/api/users/{id}", userController::delete);




        System.out.println("Server running on port http://localhost:8080");
        new HttpServer(router,8080).start();
    }
}
