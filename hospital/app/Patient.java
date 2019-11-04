package app;

import java.sql.Connection;
import java.util.Scanner;

public class Patient {
    void displayMenu() {
        Connection conn = DAL.getConn();
        Scanner scan = new Scanner(System.in);
        System.out.println("----------------------------Patient Check In--------------------------");
        System.out.println("1. Check In");
        System.out.println("2. Check Out Acknowledgement");
        System.out.println("3. Go back");
        int select = scan.nextInt();
        if (select == 1) {
            displayFacilities(conn);
        } else if (select == 2) {
//            displayCheckOutForm(conn);
        } else {
            //link to first page
        }
        //System.exit(0);
    }

    public static void displayFacilities(Connection conn) {
        System.out.println("----------------------------Facilities--------------------------");
        System.out.println("1. Pain");
        System.out.println("2. Fever");
        System.out.println("3. Other");
        System.out.println("4. Done");
        System.out.println("");
    }
}