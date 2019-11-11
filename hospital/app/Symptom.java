package app;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Scanner;

public class Symptom {
	public void addSymptomMenu(Connection conn,String staff_id) throws Exception {
		Scanner scan = new Scanner(System.in);
        System.out.println("----------------------------ADD SYMPTOM MENU --------------------------");
        System.out.println("1. Record");
        System.out.println("2. Go Back");
        
        int select = scan.nextInt();
        switch(select) {
        case 1: Record(conn,staff_id); break;
        case 2: Staff s = new Staff(staff_id); s.StaffMenuDisplay(conn); break;
       }
	}
	
	public ResultSet executeStringQuery(Connection conn, String query) throws Exception {
		//System.out.println("executing");
		PreparedStatement stmt = conn.prepareStatement(query);
		ResultSet rs = stmt.executeQuery();
		//System.out.println(rs.next());
		return rs;
	}
        
    public void Record(Connection conn,String id) throws Exception {
    	Scanner s = new Scanner(System.in);
    	System.out.println("Enter Symptom name");
    	String sym_name = s.nextLine();
    	System.out.println("Do you want to enter body part? Enter Y(Yes) or N(No)");
    	String choice = s.nextLine();
    	String body_part = "";
    	if(choice=="Y"|| choice=="Yes") {
    		body_part = s.nextLine();
    	}
    	// add entry in symptom table and get the sym_code
    	String query = "SELECT COUNT(*) FROM SYMPTOM";
    	ResultSet r = executeStringQuery(conn, query);
    	r.next();
    	int num_of_rows = r.getInt(1)+1;
    	String sym_code = "SYM00"+num_of_rows;
    	String query1;
    	if(body_part == "") {
    		query1 = "INSERT INTO SYMPTOM (CODE, NAME) VALUES ('" +sym_code + "', '"+sym_name + "')";
    	}else {
    		query1 = "INSERT INTO SYMPTOM (CODE, NAME, BP_CODE) VALUES ('" +sym_code + "', '"+sym_name + "', "+ body_part+"')";
    	}
    	ResultSet rs1 = executeStringQuery(conn, query1);
    	
    	int scale_found = enterSeverity(conn,sym_code,id);
    	while(scale_found==0) {
    		System.out.println("Enter valid severity scale");
    		scale_found = enterSeverity(conn,sym_code,id);
    	}
    }
    
    public int enterSeverity(Connection conn,String sym_code,String id) throws Exception {
    	Scanner s = new Scanner(System.in);
    	System.out.println("Enter Severity");
    	String getSeverityScales = "SELECT * FROM SEVERITY_SCALE";
    	ResultSet rs = executeStringQuery(conn, getSeverityScales);
    	ResultSet copyrs = executeStringQuery(conn, getSeverityScales);
    	while(rs.next()) {
    		System.out.println(rs.getInt(1)+ "," + rs.getString(2));
    	}
    	int scale_id = s.nextInt();
    	int scale_found = 0;
    	while(copyrs.next()) {
//			System.out.println(rs.getInt(1));
			if(copyrs.getInt(1) == scale_id) {
				System.out.println("ID matched");
				scale_found = 1;
				addEntryInSymptomSeverityTable(conn,scale_id,sym_code,id);
				break;
			}
		}
    	return scale_found;
    }
    
    public void addEntryInSymptomSeverityTable(Connection conn,int scale_id,String sym_code,String id) throws Exception {
    	String query = "INSERT INTO SYM_SEVERITY (CODE,SEVERITY) VALUES ('" +sym_code +"', "+
						scale_id+")";
		ResultSet r = executeStringQuery(conn, query);
		System.out.println("New Symptom Added!");
		addSymptomMenu(conn,id);
    }
}