package app;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Scanner;

public class App {
    public static void main(String[] args) throws Exception {
        loginDisplay();
    }

    private static void loginDisplay() throws Exception {
        Connection conn = DAL.getConn();
        Scanner scan = new Scanner(System.in);
        System.out.println("----------------------------Hospital Management System--------------------------");
        System.out.println("1. Sign In");
        System.out.println("2. Sign Up (Patient)");
        System.out.println("3. Demo Queries");
        System.out.println("3. Exit");
        int select = scan.nextInt();
        if (select == 1) {
            signinCheck(conn);
        } else {
            if (select != 2) {
                System.out.println("Enter a valid number");
                loginDisplay();
            }
            System.exit(0);
        }
    }

    private static void signinCheck(Connection conn) throws Exception {
        System.out.println("1. Sign In");
        System.out.println("2. Go Back");
        Scanner scan = new Scanner(System.in);
        int select = scan.nextInt();
        if (select == 1) {
            PreparedStatement stmt = conn.prepareStatement("SELECT NAME FROM FACILITY");
            //todo: display facilities
            System.out.println("Please choose a facility: ");
            String facility = scan.nextLine();
            System.out.println("Enter Last Name:");
            String lname = scan.nextLine();
            System.out.println("Enter DOB:");
            String dob = scan.nextLine();
            System.out.println("Enter City of Address:");
            String city = scan.nextLine();
            System.out.println("Are you a Patient?:");
            int isPatient = scan.nextInt();

            switch (isPatient) {
                case 1:
                    PreparedStatement stmt2 = conn.prepareStatement("SELECT address_id FROM patient WHERE lname=? AND dob=?");
                    stmt.setString(1, lname);
                    stmt.setString(2, dob);
                    ResultSet rs = stmt.executeQuery();
                    if (!rs.next()) {
                        System.out.println("Login Incorrect.\n");
                        loginDisplay();
                    } else {
                        //instantiate patient object
                    }
                case 2:
                    //for staff lookup
            }
        } else if (select == 2) {
            loginDisplay();
        }
    }
}
