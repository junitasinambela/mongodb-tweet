/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mycompany.mongodb.tweet;

import java.util.Scanner;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoDatabase;
import com.mongodb.ServerAddress;
import org.bson.Document;
import com.mongodb.client.FindIterable;
import com.mongodb.Block;
import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Sorts.ascending;
import java.util.ArrayList;
import java.util.Arrays;
import static java.util.Arrays.asList;
import java.util.Date;
import java.util.Iterator;


/**
 *
 * @author user
 */
public class SimpleClient {
    public MongoClient mongoClient;
    public MongoDatabase db;
    public String user;
    
    public void connect(String ip) {
        mongoClient = new MongoClient(ip, 27017);
        db = mongoClient.getDatabase("13512023");
    }
    
    public void close() {
        mongoClient.close();
    }
    
    public boolean handleCommand(String command) {
        boolean exit = false;
        String split[] = command.split(" ",2);
        
        if (split[0].equals("register") && split.length == 2) {
            String s[] = split[1].split(" ");
            if(s.length == 2){
                String username = split[1].split(" ")[0];
                String password = split[1].split(" ")[1];
                FindIterable<Document> iterable = db.getCollection("users").find(eq("username", username));
                Document doc = iterable.first();
                if(doc == null) {
                    db.getCollection("users").insertOne(
                        new Document()
                        .append("username", username)
                        .append("password", password)
                    );
                    System.out.println(username + " successfully registered");
                } else {
                    System.out.println("Username already exist");
                }
            }
            else{
                System.out.println("Usage : register <username> <password>");
            }
        } else if (split[0].equals("login") && split.length == 2) {
            String s[] = split[1].split(" ");
            if(s.length == 2){
                String username = split[1].split(" ")[0];
                String password = split[1].split(" ")[1];
                FindIterable<Document> iterable = db.getCollection("users").find(and(eq("username", username), eq("password", password)));
                Document doc = iterable.first();
                if(doc != null) {
                    user = username;
                    System.out.println("Hello, " + user + " !");
                } else {
                    System.out.println("Wrong password");
                }
            }
            else{
                System.out.println("Usage : login <username> <password>");
            }
        } else if (split[0].equals("follow") && split.length == 2) {
            if (user != null) { 
                String tofollow = split[1];
                
                FindIterable<Document> iterable = db.getCollection("users").find(eq("username", tofollow));
                Document doc = iterable.first();
                if(doc != null) {
                    db.getCollection("users").updateOne(new Document("username", user),
                                new Document("$addToSet", new Document("friends", tofollow)));
                    db.getCollection("users").updateOne(new Document("username", tofollow),
                                new Document("$addToSet", new Document("followers", user)));
                    System.out.println("Successfully follow " + tofollow);
                } else {
                    System.out.println("No user with username " + tofollow);
                }
            } else {
                System.out.println("Please login first");
            }
        } else if (split[0].equals("tweet") && split.length == 2) {
            if (user != null) {
                final String tweetBody = split[1];
                final Date date = new Date();
                db.getCollection("users").updateOne(new Document("username", user),
                        new Document("$push", new Document("tweets",
                            new Document()
                                .append("body", tweetBody)
                                .append("timestamp", date.toString()))
                        )
                );
                db.getCollection("users").updateOne(new Document("username", user),
                        new Document("$push", new Document("timeline",
                            new Document("username", user)
                                .append("body", tweetBody)
                                .append("timestamp", date.toString()))
                        )
                );
                FindIterable<Document> iterables = db.getCollection("users").find(eq("username", user));
                iterables.forEach(new Block<Document>() {
                    @Override
                    public void apply(final Document document) {
                        final ArrayList<String> followers = (ArrayList<String>) document.get("follows");
                        if(followers != null)
                            for(String follower: followers){
                                db.getCollection("users").updateOne(new Document("username", follower),
                                new Document("$push", new Document("timeline",
                                            new Document("username", user)
                                                .append("body", tweetBody)
                                                .append("timestamp", date.toString()))
                                        )
                                );
                            }
                    }
                });
            } else {
                System.out.println("Please login first");
            }
        } else if (split[0].equals("showuserline") && split.length == 2) {
            if (user != null) {
                final String toView = split[1];
                FindIterable<Document> iterable = db.getCollection("users").find(eq("username", toView));
                iterable.forEach(new Block<Document>() {
                    @Override
                    public void apply(final Document document) {
                        final ArrayList<Document> documents = (ArrayList<Document>) document.get("tweets");
                        if(documents != null)
                            for(int i = documents.size() - 1; i >= 0; i--){
                                Document d = documents.get(i);
                                System.out.println( toView +" : "+ d.getString("body"));
                            }
                    }
                });
            } else {
                System.out.println("Please login first");
            }
        } else if (split[0].equals("showtimeline")) {
            if (user != null) {
                FindIterable<Document> iterable = db.getCollection("users").find();
                iterable.forEach(new Block<Document>() {
                    @Override
                    public void apply(final Document document) {
                        final ArrayList<Document> documents = (ArrayList<Document>) document.get("timeline");
                        if(documents != null)
                            for(int i = documents.size() - 1; i >= 0; i--){
                                Document d = documents.get(i);
                                System.out.println( d.getString("username") +" : "+ d.getString("body"));
                            }
                    }
                });
            } else {
                System.out.println("Please login first");
            }
        } else if (split[0].equalsIgnoreCase("exit")){
            exit = true;
        } else {
            printUsage();
        }
        return exit;
    }
    
    public void printUsage() {
        System.out.println("Available command : ");
        System.out.println("- register <username> <password>");
        System.out.println("- login <username> <password>");
        System.out.println("- follow <username>");
        System.out.println("- tweet <tweet>");
        System.out.println("- showuserline <username>");
        System.out.println("- showtimeline");
        System.out.println("- exit");
    }
    
    public static void main (String[] args) {
        SimpleClient client = new SimpleClient();
        client.connect("167.205.35.19");
        client.user = null;
        boolean exit = false;
        client.printUsage();
        Scanner in = new Scanner(System.in);
        do {
            System.out.print("> ");
            String command = in.nextLine();
            exit = client.handleCommand(command);
        } while (!exit);
        client.close();
    }
}
