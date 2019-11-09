package app;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Scanner;

public class EnterVitalTreatPatient{
	
	public void EnterVitalMenu(Connection conn, int id) throws Exception{
		Scanner scan = new Scanner(System.in);
        System.out.println("---------------------------- ENTER VITALS TREAT PATIENT MENU --------------------------");
        System.out.println("1. Enter Vitals");
        System.out.println("2. Treat Patient");
        System.out.println("3. Go Back");
        System.out.println("4. Exit");
        int select = scan.nextInt();
        switch(select) {
        case 1: EnterVitalSigns(conn,id); break;
        case 2: TreatPatient(conn,id); break;
        case 4: conn.close();
        }
	}
	
	public ResultSet executeStringQuery(Connection conn, String query) throws Exception {
		System.out.println("executing");
		PreparedStatement stmt = conn.prepareStatement(query);
		ResultSet rs = stmt.executeQuery();
		//System.out.println(rs.next());
		return rs;
	}
	
	public void EnterVitalSigns(Connection conn, int id) throws Exception {
		String q = "SELECT CHECKIN_END FROM PATIENT_SESSION WHERE ID = "+id;
		ResultSet rs = executeStringQuery(conn, q);
		rs.next();
		if(rs.getTime(1) == null) {
			Scanner s = new Scanner(System.in);
			System.out.println("Enter the temperature of the patient");
			int temp = s.nextInt();
			System.out.println("Enter the Blood Pressure of the patient");
			System.out.println("Enter Systolic BP:");
			int systolic_bp = s.nextInt();
			System.out.println("Enter Diastolic BP");
			int diastolic_bp = s.nextInt();		
			String addVitalSigns = "UPDATE PATIENT_SESSION SET BLOOD_PRESSURE_D = " + diastolic_bp + 
					", BLOOD_PRESSURE_S = " + systolic_bp + ", TEMPERATURE = " + temp + ", CHECKIN_END = CURRENT_TIMESTAMP WHERE ID = "+ id;
			//System.out.println(addVitalSigns);
			ResultSet rs1 = executeStringQuery(conn, addVitalSigns);
			EnterVitalMenu(conn, id);
			// TODO: record timestamp trigger assessment rule
		}
		else {
			System.out.println("Patient check-in phase completed! Cannot enter vitals again!");
			EnterVitalMenu(conn, id);
		}
		
	}
	
	// TODO: get staff id from somewhere
	public void TreatPatient(Connection conn, int pid) throws Exception {
		// 1. Update the checkin end time for the user if it is null
		String updateCheckinTime = "UPDATE PATIENT_SESSION SET CHECKIN_END = CURRENT_TIMESTAMP WHERE PID ="+pid+"AND CHECKIN_END IS NOT NULL";
		ResultSet rs = executeStringQuery(conn, updateCheckinTime);
		// 2. Find if the staff can treat the patient
		String staffCanTreatBodyPart = "SELECT BP_ID FROM STAFF_HAS_BODYPART WHERE EMP_ID=1";
		ResultSet rs1 = executeStringQuery(conn, staffCanTreatBodyPart);
		String entered_bp = "SELECT BP_CODE FROM PATIENT_SYM_MAPPING WHERE SID = "+pid;
		ResultSet rs2 = executeStringQuery(conn, entered_bp);
		String bp_from_symptom = "SELECT SYMPTOM.BP_CODE FROM SYMPTOM INNER JOIN PATIENT_SYM_MAPPING ON SYMPTOM.CODE = PATIENT_SYM_MAPPING.SYM_CODE"+
		" AND PATIENT_SYM_MAPPING.BP_CODE IS NULL AND PATIENT_SYM_MAPPING.SID = "+pid;
		ResultSet rs3 = executeStringQuery(conn, bp_from_symptom);
		HashSet<String> body_part_patient = new HashSet<String>();
		HashSet<String> body_part_staff = new HashSet<String>();
		while(rs2.next()) {
			body_part_patient.add(rs2.getString(1));
		}
		while(rs3.next()) {
			body_part_patient.add(rs3.getString(1));
		}
		while(rs1.next()) {
			body_part_staff.add(rs1.getString(1));
		}
		if(body_part_staff.containsAll(body_part_patient)) {
			// set the treatment time for the patient
			String updateTreatInPatientSession = "UPDATE PATIENT_SESSION SET TREATED='Y',TREATMENT_TIME=CURRENT_TIMESTAMP";
			ResultSet r = executeStringQuery(conn, updateTreatInPatientSession);
		}
		else {
			System.out.println("Inadequate Privilege");
		}
	}
}