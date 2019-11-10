package app;

//import java.sql.;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Scanner;

public class App {
    public static void main(String[] args) throws Exception {
    	Connection conn = DriverManager.getConnection(
                "jdbc:oracle:thin:@orca.csc.ncsu.edu:1521:orcl01", "ssmehend", "200262272");
		if(conn==null) {
			System.out.println("Connection NULL");
		}
        loginDisplay(conn);
    }

    private static void loginDisplay(Connection conn) throws Exception {
        Scanner scan = new Scanner(System.in);
        System.out.println("----------------------------Hospital Management System--------------------------");
        System.out.println("1. Sign In");
        System.out.println("2. Sign Up (Patient)");
        System.out.println("3. Demo Queries");
        System.out.println("4. Exit");
        int select = scan.nextInt();
        switch (select) {
            case 1:
                signIn(conn);
            case 2:
                signUp(conn);
            case 3:
            case 4:
            	System.out.println("Connection closed");
            	conn.close();
                System.exit(0);
                scan.close();
            default:
                System.out.println("Enter a valid number");
                loginDisplay(conn);
        }
    }

    static void signIn(Connection conn) throws Exception {
        Scanner scan = new Scanner(System.in);

        HashMap<Integer,String> facilities = new HashMap();
        PreparedStatement stmt;
        stmt = conn.prepareStatement("SELECT NAME,FACILITY_ID FROM HOSPITAL");
        ResultSet rs = stmt.executeQuery();
        while (rs.next()) {
            facilities.put(rs.getInt("FACILITY_ID"),rs.getString("NAME"));
        }

        System.out.println("Facilities available (ID: NAME):");
        for (Entry<Integer, String> map : facilities.entrySet()) {
            System.out.println(map.getKey()+": "+map.getValue());
        }
        System.out.println("Please choose a facility id:");
        int facilityid = scan.nextInt();
        scan.nextLine();
        System.out.println("Enter Last Name:");
        String lname = scan.nextLine();
        System.out.println("Enter DOB (DD-mmm-YY):");
        String dob = scan.nextLine();
        System.out.println("Enter City of Address:");
        String city = scan.nextLine();
        System.out.println("Are you a Patient? (1 or 0):");
        int isPatient = scan.nextInt();

        System.out.println("1. Sign In");
        System.out.println("2. Go Back");
        int select = scan.nextInt();
        if (select == 1) {
            switch (isPatient) {
                case 1:
                    PreparedStatement stmtPatient = conn.prepareStatement("select * from patient where address_id in (SELECT id from address where city = ?) and dob = ? and lname = ? and facility_id = ?");
                    stmtPatient.setString(1, city);
                    stmtPatient.setDate(2, new java.sql.Date(new SimpleDateFormat("dd-MMM-yy").parse(dob).getTime()));
                    stmtPatient.setString(3, lname);
                    stmtPatient.setInt(4, facilityid);
                    ResultSet rs1 = stmtPatient.executeQuery();
                    if (!rs1.next()) {
                        System.out.println("Login incorrect as it seems you are here for the first time. Please sign up.");
                        loginDisplay(conn);
                    } else {
                        PreparedStatement getPatientID = conn.prepareStatement("select patient_id_seq.currval from dual");
                        ResultSet rs3 = getPatientID.executeQuery();
                        int seqPatient = 0;
                        while (rs3.next()) {
                            seqPatient = rs3.getInt("CURRVAL");
                        }
                        System.out.println("Login Successful");
                        Patient p = new Patient(seqPatient);
                        p.displayMenu();
                    }
                case 0:
                    PreparedStatement stmtStaff = conn.prepareStatement("select employee_id from staff where address_id in (SELECT id from address where city = ?) and dob = ? and lname = ? and facility_id = ?");
                    stmtStaff.setString(1, city);
                    stmtStaff.setDate(2, new java.sql.Date(new SimpleDateFormat("dd-MMM-yy").parse(dob).getTime()));
                    stmtStaff.setString(3, lname);
                    stmtStaff.setInt(4, facilityid);
                    ResultSet rs2 = stmtStaff.executeQuery();
                    if (!rs2.next()) {
                        System.out.println("Login Incorrect\n");
                        loginDisplay(conn);
                    } else {
                        String employeeID = "";
                        while(rs.next()){
                            employeeID = rs.getString("EMPLOYEE_ID");
                        }
                        System.out.println("Login Successful");
                        // Remember to replace patient by staff
                        Staff s = new Staff(employeeID);
                        s.StaffMenuDisplay(conn);
                    }
            }
        } else if (select == 2) {
            loginDisplay(conn);
        }
        scan.close();
    }

    private static void signUp(Connection conn) throws Exception {
        Scanner scan = new Scanner(System.in);
        System.out.println("A. First Last Name");
        String[] name = scan.nextLine().split(" ");
        String fname = name[0].trim();
        String lname = name[1].trim();
        System.out.println("B. Date of Birth (DD-mmm-YY)");
        String dob = scan.nextLine().trim();
        System.out.println("C. Address");
        String address = scan.nextLine();
        String[] addressFields = address.split(",");
        String streetName = addressFields[0].trim();
        String city = addressFields[1].trim();
        String state = addressFields[2].trim();
        String country = addressFields[3].trim();
        System.out.println("D. Phone Number:");
        String contact = scan.nextLine().trim();
        System.out.println("E. Facility ID:");
        HashMap<Integer,String> facilities = new HashMap<Integer,String>();
        PreparedStatement stmt;
        stmt = conn.prepareStatement("SELECT NAME,FACILITY_ID FROM HOSPITAL");
        ResultSet rs = stmt.executeQuery();
        while (rs.next()) {
            facilities.put(rs.getInt("FACILITY_ID"),rs.getString("NAME"));
        }
        System.out.println("Facilities available (ID: NAME):");
        for (Entry<Integer, String> map : facilities.entrySet()) {
            System.out.println(map.getKey()+": "+map.getValue());
        }
        System.out.println("Please insert your facility id now");
        int facilityID = scan.nextInt();
        scan.nextLine();
        System.out.println("1. Sign Up");
        System.out.println("2. Go Back");
        int select = scan.nextInt();

        if (select == 1) {
        	//Todo: Check if entered address exists in the 
            PreparedStatement checkAddress = conn.prepareStatement("select id from address where street_name=? and city=? and state=? and country=?");
            checkAddress.setString(1, streetName);
            checkAddress.setString(2, city);
            checkAddress.setString(3, state);
            checkAddress.setString(4, country);
            ResultSet rs4 = checkAddress.executeQuery();
            int seqAdd = -1;

            if( rs4.next()){
            	//System.out.println("Found address in DB");
            	seqAdd = rs4.getInt("ID");
            }
            else{
            	//System.out.println("Did not Find address in DB");
            	PreparedStatement insertAddress = conn.prepareStatement("insert into address(street_name, city, state, country) values(?,?,?,?)");
                insertAddress.setString(1, streetName);
                insertAddress.setString(2, city);
                insertAddress.setString(3, state);
                insertAddress.setString(4, country);
                insertAddress.executeQuery();
                PreparedStatement getAddSeqID = conn.prepareStatement("select address_id_seq.currval from dual");
                ResultSet rs3 = getAddSeqID.executeQuery();
                while (rs3.next()) {
                    seqAdd = rs3.getInt("CURRVAL");
                }
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
            System.out.println("Sign Up Successful");

        } else if (select == 2) {
            loginDisplay(conn);
        } else {
            System.out.println("Enter a valid number");
            signUp(conn);
        }
        scan.close();
    }
}