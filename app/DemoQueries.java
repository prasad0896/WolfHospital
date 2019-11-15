package app;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Scanner;

public class DemoQueries {
	public void menu(Connection conn) throws Exception {
		System.out.println("---------------------DEMO QUERIES----------------------");
		System.out.println("1.Find all patients that were discharged but had negative experiences at any facility, list their names," + 
				"facility, check-in date, discharge date and negative experiences");
		System.out.println("2. Find facilities that did not have a negative experience for a specific period (to be given).");
		System.out.println("3. For each facility, find the facility that is sends the most referrals to.");
		System.out.println("4. Find facilities that had no negative experience for patients with cardiac symptoms");
		System.out.println("5. Find the facility with the most number of negative experiences (overall i.e. of either kind)");
		System.out.println("6. Find each facility, list the patient encounters with the top five longest check-in phases (i.e. time from\n" + 
				"begin check-in to when treatment phase begins (list the name of patient, date, facility, duration and\n" + 
				"list of symptoms).");
		System.out.println("\n\n Enter your choice.");
		Scanner s = new Scanner(System.in);
		int choice = s.nextInt();
		switch(choice) {
			case 1: execOne(conn); break;
			case 2: execTwo(conn); break;
			case 3: execThree(conn); break;
		}
	}
	
	public ResultSet executeStringQuery(Connection conn, String query) throws Exception {
		//System.out.println("executing");
		PreparedStatement stmt = conn.prepareStatement(query);
		ResultSet rs = stmt.executeQuery();
		//System.out.println(rs.next());
		return rs;
	}
	public void printResult(ResultSet rs) throws Exception {
		ResultSetMetaData metadata = rs.getMetaData();
		int colCount= metadata.getColumnCount();
		while(rs.next()) {
			for(int i=0;i<colCount;i++) {
				  System.out.print(rs.getString(i) + " "); 
			}
			System.out.println();
		}
	}
	
	
	public void execOne(Connection conn) throws Exception {
		String query1 = "select patient.F_NAME, patient.L_NAME, patient.FACILITY_ID, patient_session.checkin_end,patient_session.checkout_date, negative_exp.description"+
						" from patient inner join PATIENT_SESSION on patient.pid=patient_session.pid inner join report on PATIENT_SESSION.id=REPORT.patient_id inner join negative_exp"+
						" on report.negative_exp_id=negative_exp.n_id";
		ResultSet rs = executeStringQuery(conn, query1);
		printResult(rs);
	}
	
	public void execTwo(Connection conn) throws Exception {
		Scanner s = new Scanner(System.in);
		System.out.println("Enter start date (DD-mmm-YY");
		String start_date = s.nextLine();
		System.out.println("Enter end date (DD-mmm-YY");
		String end_date = s.nextLine();
		String query2 = "select patient_session.facility_id from patient_session inner join report on report.patient_id = patient_session.sid where report.negative_exp_id=0 and patient_session.treatment_time between <date range>" ;
		ResultSet rs = executeStringQuery(conn, query2);
		printResult(rs);
	}
	
	public void execThree(Connection conn) {
		String query3 = "select count(referral_status.facility_id), staff.facility_id from referral_status inner join staff on referral_status.employee_id=staff.employee_id GROUP BY staff.facility_id";
	}
}