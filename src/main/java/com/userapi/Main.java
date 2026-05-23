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
        var dataSource= DatabaseConfig.getDataSource();

        var userRepo=new UserRepository(dataSource);
        var userService=new UserService(userRepo);
        var userController=new UserController(userService);

        if(args.length>0&& args[0].equals("---seed")){
            new DataSeeder(dataSource).seed(1_000_000);
            System.out.println("Seeding complete. Existing.");
            return;
        }

        var router=new Router();
        router.get("/api/users", userController::getAll);
        router.get("/api/users/{id}", userController::getById);
        router.post("/api/users", userController::create);
        router.put("/api/users/{id}", userController::update);
        router.delete("/api/users/{id}", userController::delete);



        new HttpServer(router,8080).start();
        System.out.println("Server running on port http://localhost:8080");
    }
}
