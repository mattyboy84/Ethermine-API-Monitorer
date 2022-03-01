package com.company;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

public class Main {

    static String walletAddress = "0x426cc32371ef10e8052ccf69c5fbdd6dfac0a474";


    public static void main(String[] args) {
        walletAddress = (args.length > 0) ? (args[0]) : (walletAddress);
        //https://api.ethermine.org/docs/
        String urlString = "https://api.ethermine.org/miner/" + walletAddress + "/currentstats";//current stats contains M/H info
        //System.out.println(urlString);

        JsonObject jsonObject;
        Connection connection = null;

        try {//initial setup of the sql connection
            BufferedReader br = new BufferedReader(new FileReader("src\\password.txt"));//read in password from a txt file
            String password = br.readLine();
            //
            connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/mysql", "root", password);
        } catch (Exception e) {
        }
        //
        //
        //
        try {//tests if this is a first time waller address - if so, will create its table
            Statement test = connection.createStatement();
            test.execute("select dateTime from miner_results_"+walletAddress+" where dateTime < \"" + DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT) + "\";");
        } catch (Exception e) {
            try{
                System.out.println("First time wallet used");
                //
                Statement test = connection.createStatement();
                test.execute("create table miner_results_"+walletAddress+ "(dateTime varchar(100),reportedHash float,currentHash float, averageHash float);");
                //
                System.out.println("Created table for first time wallet");
            }catch (Exception e1){

            }
        }


        while (true) {//sets up the loop to call API every x minutes

            try {
                jsonObject = getData(urlString);

                //System.out.println(jsonObject);

                LocalDateTime currentDate = LocalDateTime.now();
                float reportedHash = jsonObject.get("data").getAsJsonObject().get("reportedHashrate").getAsFloat() / (1000 * 1000);
                float currentHashrate = jsonObject.get("data").getAsJsonObject().get("currentHashrate").getAsFloat() / (1000 * 1000);
                float averageHashrate = jsonObject.get("data").getAsJsonObject().get("averageHashrate").getAsFloat() / (1000 * 1000);
                //
                System.out.println("---");
                System.out.println("Time: " + currentDate.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)) + " reported: " + reportedHash + " Current: " + currentHashrate + " Average: " + averageHashrate);
                //reported is the most accurate

                insertIntoSQL(currentDate.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)), reportedHash, currentHashrate, averageHashrate, connection);

                Thread.sleep((5*60*1000));//5 minutes
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    private static void insertIntoSQL(String dateTime, float reportedHash, float currentHashrate, float averageHashrate, Connection connection) {
        try {
            Statement statement = connection.createStatement();
            String state = "insert into miner_results_"+walletAddress+" values (" + "\"" + dateTime + "\"" + "," + reportedHash + "," + currentHashrate + "," + averageHashrate + ")";
            //System.out.println(state);
            statement.execute(state);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static JsonObject getData(String urlString) {
        try {
            StringBuilder result = new StringBuilder();
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line);
            }
            reader.close();
            //
            return new JsonParser().parse(String.valueOf(result)).getAsJsonObject();

        } catch (IOException e) {
            System.out.println(e);
        }
        return null;
    }
}