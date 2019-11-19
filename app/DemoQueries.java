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
			case 4: execFour(conn); break;
			case 5: execFive(conn); break;
			case 6: execSix(conn); break;
		}
	}
	
	public void execOne(Connection conn) throws Exception {
		String query1 = "select patient.F_NAME, patient.L_NAME, patient.FACILITY_ID, patient_session.checkin_end,patient_session.checkout_date, negative_exp.description from patient inner join PATIENT_SESSION on patient.pid=patient_session.pid inner join report on PATIENT_SESSION.id=REPORT.patient_id inner join negative_exp on report.negative_exp_id=negative_exp.n_id";
		ResultSet rs = executeStringQuery(conn, query1);
		printResult(rs);
	}
	
	public void execTwo(Connection conn) throws Exception {
		Scanner s = new Scanner(System.in);
		System.out.println("Enter start date (DD-mmm-YY");
		String start_date = s.nextLine();
		System.out.println("Enter end date (DD-mmm-YY");
		String end_date = s.nextLine();
		String query2 = "select unique(patient_session.facility_id) from patient_session inner join report on report.patient_id = patient_session.id where report.negative_exp_id is NULL and patient_session.treatment_time between '"+ start_date+"' and '" +end_date+"'" ;
		ResultSet rs = executeStringQuery(conn, query2);
		printResult(rs);
	}
	
	public void execThree(Connection conn) throws Exception {
		String query3 = "select count(referral_status.facility_id), staff.facility_id from referral_status inner join staff on referral_status.employee_id=staff.employee_id GROUP BY staff.facility_id";
		ResultSet rs = executeStringQuery(conn, query3);
		printResult(rs);
	}
	
	private void execSix(Connection conn) throws Exception {
		String query6 = "select * from patient where pid in (select pid from (" + 
				"select patient_session.pid, (checkin_end - checkin_start) diff from patient_session" + 
				"where checkin_start is not null and checkin_end is not null and treated = 'Y' and rownum <= 5 order by diff desc" + 
				"))";
		ResultSet rs = executeStringQuery(conn, query6);
		printResult(rs);
		
	}

	private void execFive(Connection conn) throws Exception {
		String query5 = "select * from hospital where facility_id in ( select facility_id from (" + 
				"select ps.facility_id, count(ps.facility_id) as c from patient_session ps inner join report on report.patient_id = ps.id" + 
				"where report.negative_exp_id is not null group by ps.facility_id order by c desc))";
		ResultSet rs = executeStringQuery(conn, query5);
		printResult(rs);
	}

	private void execFour(Connection conn) throws Exception {
		String query4 = "select * from hospital where facility_id not in (" + 
				"select ps.facility_id from patient_session ps inner join report on report.patient_id = ps.id where ps.id in (" + 
				"select ps.id from patient_session ps inner join patient_sym_mapping psm on psm.sid = ps.id where bp_code in ( " + 
				"select code from bodypart where name ='Chest')) and report.negative_exp_id is not null)" + 
				"and facility_id in (select ps.facility_id from patient_session ps inner join report on report.patient_id = ps.id where ps.id in (" + 
				"select ps.id from patient_session ps inner join patient_sym_mapping psm on psm.sid = ps.id where bp_code in ( " + 
				"select code from bodypart where name ='Heart')))";
		ResultSet rs = executeStringQuery(conn, query4);
		printResult(rs);
		
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

}