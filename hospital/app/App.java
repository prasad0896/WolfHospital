package app;

//import java.sql.;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Scanner;

public class App {
    public static void main(String[] args) throws Exception {
        loginDisplay();
    }

    private static void loginDisplay() throws Exception {
        Scanner scan = new Scanner(System.in);
        System.out.println("----------------------------Hospital Management System--------------------------");
        System.out.println("1. Sign In");
        System.out.println("2. Sign Up (Patient)");
        System.out.println("3. Demo Queries");
        System.out.println("4. Exit");
        int select = scan.nextInt();
        switch (select) {
            case 1:
                signIn();
            case 2:
                signUp();
            case 3:
            case 4:
                System.exit(0);
            default:
                System.out.println("Enter a valid number");
                loginDisplay();
        }
    }

    private static void signIn() throws Exception {
        Scanner scan = new Scanner(System.in);

        HashMap<Integer,String> facilities = new HashMap<Integer,String>();
        PreparedStatement stmt;
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(
                    "jdbc:oracle:thin:@orca.csc.ncsu.edu:1521:orcl01", "ssmehend", "200262272");
            assert conn != null;
            stmt = conn.prepareStatement("SELECT NAME,FACILITY_ID FROM HOSPITAL");
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                facilities.put(rs.getInt("FACILITY_ID"),rs.getString("NAME"));
            }
        } finally {
            assert conn != null;
            conn.close();
        }

        System.out.println("Facilities available (ID: NAME):");
        for (Entry<Integer, String> map : facilities.entrySet()) {
            System.out.println(map.getKey()+": "+map.getValue());
        }
        System.out.println("Please choose a facility id:");
        int facilityid = scan.nextInt();
        System.out.println("Enter Last Name:");
        String lname = scan.nextLine();
        System.out.println("Enter DOB (DD-MM-YY):");
        String dob = scan.nextLine();
        System.out.println("Enter City of Address:");
        String city = scan.nextLine();
        System.out.println("Are you a Patient?:");
        int isPatient = scan.nextInt();

        System.out.println("1. Sign In");
        System.out.println("2. Go Back");
        int select = scan.nextInt();
        if (select == 1) {

            conn = DriverManager.getConnection(
                    "jdbc:oracle:thin:@orca.csc.ncsu.edu:1521:orcl01", "ssmehend", "200262272");
            switch (isPatient) {
                case 1:
                    PreparedStatement stmtPatient = conn.prepareStatement("select * from patient where address_id in (SELECT id from address where city = ?) and dob = ? and lname = ? and facility_id = ?");
                    stmtPatient.setString(1, city);
                    stmtPatient.setDate(2, new java.sql.Date(new SimpleDateFormat("dd-MMM-yy").parse(dob).getTime()));
                    stmtPatient.setString(3, lname);
                    stmtPatient.setInt(4, facilityid);
                    ResultSet rs1 = stmtPatient.executeQuery();
                    if (!rs1.next()) {
                        System.out.println("Login Incorrect\n");
                        loginDisplay();
                    } else {
                        System.out.println("Login Successful");
                        Patient p = new Patient();
                        p.displayMenu();
                    }
                case 2:
                    PreparedStatement stmtStaff = conn.prepareStatement("select * from staff where address_id in (SELECT id from address where city = ?) and dob = ? and lname = ? and facility_id = ?");
                    stmtStaff.setString(1, city);
                    stmtStaff.setDate(2, new java.sql.Date(new SimpleDateFormat("dd-MMM-yy").parse(dob).getTime()));
                    stmtStaff.setString(3, lname);
                    stmtStaff.setInt(4, facilityid);
                    ResultSet rs2 = stmtStaff.executeQuery();
                    if (!rs2.next()) {
                        System.out.println("Login Incorrect\n");
                        loginDisplay();
                    } else {
                        System.out.println("Login Successful");
                        Patient p = new Patient();
                        p.displayMenu();
                    }
            }
            conn.close();
        } else if (select == 2) {
            loginDisplay();
        }
        scan.close();
    }

    private static void signUp() throws Exception {
        Scanner scan = new Scanner(System.in);
        System.out.println("A. First Last Name");
        String[] name = scan.nextLine().split(" ");
        String fname = name[0].strip();
        String lname = name[1].strip();
        System.out.println("B. Date of Birth");
        String dob = scan.nextLine();
        System.out.println("C. Address");
        String address = scan.nextLine();
        String[] addressFields = address.split(",");
        String streetName = addressFields[0].strip();
        String city = addressFields[1].strip();
        String state = addressFields[2].strip();
        String country = addressFields[3].strip();
        System.out.println("D. Phone Number:");
        String contact = scan.nextLine();
        System.out.println("E. Facility ID:");
        HashMap<Integer,String> facilities = new HashMap<Integer,String>();
        PreparedStatement stmt;
        Connection conn1 = null;
        try {
            conn1 = DriverManager.getConnection(
                    "jdbc:oracle:thin:@orca.csc.ncsu.edu:1521:orcl01", "ssmehend", "200262272");
            assert conn1 != null;
            stmt = conn1.prepareStatement("SELECT NAME,FACILITY_ID FROM HOSPITAL");
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                facilities.put(rs.getInt("FACILITY_ID"),rs.getString("NAME"));
            }
        } finally {
            assert conn1 != null;
            conn1.close();
        }

        System.out.println("Facilities available (ID: NAME):");
        for (Entry<Integer, String> map : facilities.entrySet()) {
            System.out.println(map.getKey()+": "+map.getValue());
        }
        System.out.println("Please insert your facility id");
        int facilityID = scan.nextInt();
        System.out.println("1. Sign Up");
        System.out.println("2. Go Back");
        int select = scan.nextInt();

        if (select == 1) {
        	//Todo: Check if entered address exists in the 
            Connection conn = DriverManager.getConnection(
                    "jdbc:oracle:thin:@orca.csc.ncsu.edu:1521:orcl01", "ssmehend", "200262272");
            PreparedStatement insertAddress = conn.prepareStatement("insert into address(street_name, city, state, country) values(?,?,?,?)");
            insertAddress.setString(1, streetName);
            insertAddress.setString(2, city);
            insertAddress.setString(3, state);
            insertAddress.setString(4, country);
            insertAddress.executeQuery();
            PreparedStatement getAddSeqID = conn.prepareStatement("select address_id_seq.currval from dual");
            ResultSet rs3 = getAddSeqID.executeQuery();
            int seqAdd = 0;
            while (rs3.next()) {
                seqAdd = rs3.getInt("CURRVAL");
            }
            PreparedStatement insertPatient = conn.prepareStatement("insert into patient(FNAME, LNAME, DOB, PHONENUMBER, ADDRESS_ID, FACILITY_ID) values(?,?,?,?,?,?)");
            insertPatient.setString(1, fname);
            insertPatient.setString(2, lname);
            insertPatient.setString(3, dob);
            insertPatient.setString(4, contact);
            insertPatient.setInt(5, seqAdd);
            insertPatient.setInt(6, facilityID);
            insertPatient.executeQuery();
            conn.close();
        } else if (select == 2) {
            loginDisplay();
        } else {
            System.out.println("Enter a valid number");
            signUp();
        }
        scan.close();
    }
}
