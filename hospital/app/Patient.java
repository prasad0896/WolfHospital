package app;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Scanner;

public class Patient {

    private final int id;

    Patient(int id) {
        this.id = id;
    }

    void displayMenu() throws Exception {
        Connection conn = DAL.getConn();
        Scanner scan = new Scanner(System.in);
        System.out.println("----------------------------Patient Check In--------------------------");
        System.out.println("1. Check In");
        System.out.println("2. Check Out Acknowledgement");
        System.out.println("3. Go back");
        int select = scan.nextInt();
        if (select == 1) {
            displayCheckIn(conn);
        } else if (select == 2) {
//            displayCheckOutForm(conn);
        } else {
            App.signIn();
        }
        scan.close();
        //System.exit(0);
    }

    void displayCheckIn(Connection conn) throws SQLException {
        Scanner scan = new Scanner(System.in);
        System.out.println("----------------------------Facilities--------------------------");
        System.out.println("1. Pain");
        System.out.println("2. Fever");
        System.out.println("3. Headache");
        System.out.println("4. Tenderness");
        System.out.println("5. Tightness");
        System.out.println("6. Numbness");
        System.out.println("7. Lightheadedness");
        System.out.println("8. Shortness of Breath");
        System.out.println("8. Blurred Vision");
        System.out.println("9. Other");
        System.out.println("10. Done");
        int select = scan.nextInt();
        if (select >= 1 && select <= 8) {
            switch (select){
                case 1:
                case 2:
                case 3:
                case 4:
                case 5:
                case 6:
                case 7:
                case 8:
            }
            displayMetaData("");
        } else if (select == 9) {
            System.out.println("Enter symptom name:");
            String symtom_name = scan.nextLine();
            String symtom_code = symtom_name.substring(0,3).toUpperCase();
            displayMetaData(symtom_name, symtom_code);
        } else if (select == 10) {
            validatePatient();
        }
        System.out.println("");
    }

    void displayMetaData(String symtom_name) {
        System.out.println("1. Body part");
        System.out.println("2. Duration");
        System.out.println("3. Reoccurring");
        System.out.println("4. Severity");
        System.out.println("5. Cause (Incident)");
    }

    void displayMetaData(String symtom_name, String symtom_code) {
//        PreparedStatement stmtPatient = conn.prepareStatement("insert into symptom (body_part_code, code, name) values (?,?,?)");
//        stmtPatient.setString(1, body_part_code);
//        stmtPatient.setString(1, symtom_code);
//        stmtPatient.setString(1, symtom_name);
        System.out.println("1. Body part");
        System.out.println("2. Duration");
        System.out.println("3. Reoccurring");
        System.out.println("4. Severity");
        System.out.println("5. Cause (Incident)");
    }

    void validatePatient() throws SQLException {
        Connection conn = DriverManager.getConnection(
                "jdbc:oracle:thin:@orca.csc.ncsu.edu:1521:orcl01", "ssmehend", "200262272");
        PreparedStatement insertAddress = conn.prepareStatement("select pid from patient_sym_mapping where pid = ?");
        insertAddress.setInt(1, this.id);
    }
}