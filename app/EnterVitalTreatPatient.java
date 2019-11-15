package app;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Scanner;


public class EnterVitalTreatPatient{
	
	public void EnterVitalMenu(Connection conn, int id, String staff_id) throws Exception{
		// only medical staff has access
		String check_medical = "SELECT DESIGNATION FROM STAFF WHERE EMPLOYEE_ID = "+staff_id;
		ResultSet medical_or_not = executeStringQuery(conn, check_medical);
		medical_or_not.next();
		if(medical_or_not.getString(1).equalsIgnoreCase("non-medical")) {
			System.out.println("Access Denied. Only Medical Staff has access. Going back!\n");
			Staff s = new Staff(staff_id);
			s.StaffMenuDisplay(conn);
		}
		Scanner scan = new Scanner(System.in);
        System.out.println("---------------------------- STAFF PROCESS PATIENT MENU --------------------------");
        System.out.println("1. Enter Vitals");
        System.out.println("2. Treat Patient");
        System.out.println("3. Go Back");
        System.out.println("4. Exit");
        int select = scan.nextInt();
        switch(select) {
        case 1: EnterVitalSigns(conn,id,staff_id); break;
        case 2: TreatPatient(conn,id,staff_id); break;
        case 3: Staff s = new Staff(staff_id); s.StaffMenuDisplay(conn); break;
        case 4: conn.close();
        }
	}
	
	public ResultSet executeStringQuery(Connection conn, String query) throws Exception {
		//System.out.println("executing");
		PreparedStatement stmt = conn.prepareStatement(query);
		ResultSet rs = stmt.executeQuery();
		//System.out.println(rs.next());
		return rs;
	}
	
	public void EnterVitalSigns(Connection conn, int id, String staff_id) throws Exception {
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
			System.out.println("---------------------------- STAFF ENTER VITAL MENU --------------------------");
	        System.out.println("1. Record");
	        System.out.println("2. Go Back");
	        int choice = s.nextInt();
	        if(choice == 1) {
			String addVitalSigns = "UPDATE PATIENT_SESSION SET BLOOD_PRESSURE_D = " + diastolic_bp + 
					", BLOOD_PRESSURE_S = " + systolic_bp + ", TEMPERATURE = " + temp + ", CHECKIN_END = CURRENT_TIMESTAMP WHERE ID = "+ id;
			//System.out.println(addVitalSigns);
			ResultSet rs1 = executeStringQuery(conn, addVitalSigns);
			triggerAssessmentRule(conn, id);
			EnterVitalMenu(conn, id,staff_id);
	        }
	        else if(choice == 2) {
	        	EnterVitalMenu(conn, id,staff_id);
	        }
		}
		else {
			System.out.println("Patient check-in phase completed! Cannot enter vitals again!");
			EnterVitalMenu(conn, id,staff_id);
		}	
}
	
	public void triggerAssessmentRule(Connection conn,int pid) throws Exception {
		String priority = "Normal";
		int matched_size = 0;
		String getPatientSymptom = "SELECT SYM_CODE,SEVERITY,BP_CODE FROM PATIENT_SYM_MAPPING WHERE SID = "+pid;
		ResultSet rs = executeStringQuery(conn, getPatientSymptom);
		// symptoms-bpcode and severity as entered by patient
		HashMap<String,String> symptom_severity = new HashMap<>();
		while(rs.next()) {
			if(rs.getString(3)!=null) {
			symptom_severity.put(rs.getString(1)+"-"+rs.getString(3), rs.getString(2));
			} else {
				symptom_severity.put(rs.getString(1), rs.getString(2));
			}	
		}
		String getAssessmentRule = "SELECT * FROM ASSESSMENT_RULE ";
		ResultSet rs_rule = executeStringQuery(conn, getAssessmentRule);
		//ArrayList<String> matchedRules = new ArrayList<String>();
		while(rs_rule.next()) {
			String rule = rs_rule.getString(2);
			System.out.println(rule);
			String [] rule_has_syms_and_scale = rule.split(" & ");
			System.out.println(Arrays.deepToString(rule_has_syms_and_scale));
			HashMap<String,String> rule_sym_scale = new HashMap<String, String>();
			//store all symptoms present in the assessment rule in a arraylist
			for(String sym: rule_has_syms_and_scale) {
				String[] sym_scale = sym.split(" ");
				rule_sym_scale.put(sym_scale[0], sym_scale[1]+" "+sym_scale[2]);
			}
			boolean skip_iteration = false;
			if((symptom_severity.keySet()).containsAll(rule_sym_scale.keySet())) {
				for(Map.Entry<String,String> entry: rule_sym_scale.entrySet()) {
					String[] scale_in_rule = entry.getValue().split(" "); // >= 2  OR = high
					//System.out.println(Arrays.deepToString(scale_in_rule));
					String[] sym_in_rule = entry.getKey().split("-"); // key is sym-bpcode
					//System.out.println(Arrays.deepToString(sym_in_rule));
					//String bp_code_in_rule = sym_in_rule[1];
					String scale_patient;
					if(sym_in_rule.length == 2) {
						scale_patient = symptom_severity.get(sym_in_rule[0]+"-"+sym_in_rule[1]);
					} else {
						scale_patient = symptom_severity.get(sym_in_rule[0]);
					}
					switch(scale_in_rule[0]) {
					case ">": if(Integer.parseInt(scale_patient) > Integer.parseInt(scale_in_rule[1])) {
							continue;
							} else{
								skip_iteration = true;
							}break;
					case "<":  if(Integer.parseInt(scale_patient) < Integer.parseInt(scale_in_rule[1])) {
						continue;
						} else{
							skip_iteration = true;
						}break;
					case ">=":  if(Integer.parseInt(scale_patient) >= Integer.parseInt(scale_in_rule[1])) {
						continue;
						} else{
							skip_iteration = true;
						}break;
					case "<=" :  if(Integer.parseInt(scale_patient) <= Integer.parseInt(scale_in_rule[1])) {
						continue;
						} else{
							skip_iteration = true;
						}break;
					case "=":  if(scale_patient.equals(scale_in_rule[1])) {
						continue;
						} else{
							skip_iteration = true;
						}break;
					}
					if(skip_iteration == true) {
						break;
					}
				}
				if(skip_iteration == false && rule_sym_scale.size() > matched_size) {
					priority = rs_rule.getString(3);
				}
		}
			
	}
		System.out.println("Priority is " + priority);
		String set_priority = "UPDATE PATIENT_SESSION SET PRIORITY = '"+priority + "' WHERE ID = "+pid;
		ResultSet r = executeStringQuery(conn, set_priority);
}
	
	public void TreatPatient(Connection conn, int pid, String staff_id) throws Exception {
		// 1. Update the checkin end time for the user if it is null
		String updateCheckinTime = "UPDATE PATIENT_SESSION SET CHECKIN_END = CURRENT_TIMESTAMP WHERE PID ="+pid+" AND CHECKIN_END IS NOT NULL";
		ResultSet rs = executeStringQuery(conn, updateCheckinTime);
		// trigger assessmentRule
		String checkPriority = "SELECT PRIORITY FROM PATIENT_SESSION WHERE ID = "+pid;
		ResultSet rs11 = executeStringQuery(conn, checkPriority);
		rs11.next();
		if(rs11.getString(1)==null) {
			triggerAssessmentRule(conn,pid);
		}
		// 2. Find if the staff can treat the patient
		String staffCanTreatBodyPart = "SELECT BP_ID FROM STAFF_HAS_BODYPART WHERE EMP_ID="+staff_id;
		ResultSet rs1 = executeStringQuery(conn, staffCanTreatBodyPart);
		
		String entered_bp = "SELECT BP_CODE FROM PATIENT_SYM_MAPPING WHERE SID = "+pid;
		ResultSet rs2 = executeStringQuery(conn, entered_bp);
		
		String bp_from_symptom = "SELECT SYMPTOM.BP_CODE FROM SYMPTOM INNER JOIN PATIENT_SYM_MAPPING ON SYMPTOM.CODE = PATIENT_SYM_MAPPING.SYM_CODE"+
		" AND PATIENT_SYM_MAPPING.BP_CODE IS NULL AND PATIENT_SYM_MAPPING.SID = "+pid;
		ResultSet rs3 = executeStringQuery(conn, bp_from_symptom);
		
		HashSet<String> body_part_patient = new HashSet<String>();
		HashSet<String> body_part_staff = new HashSet<String>();
		while(rs2.next()) {
			if(rs2.getString(1)!=null) {
			body_part_patient.add(rs2.getString(1));
			System.out.println(rs2.getString(1));
			}
		}
		while(rs3.next()) {
			if(rs3.getString(1)!=null) {
			body_part_patient.add(rs3.getString(1));
			System.out.println(rs3.getString(1));
			}
		}
		while(rs1.next()) {
			if(rs1.getString(1)!=null) {
				body_part_staff.add(rs1.getString(1));
			}
		}
		System.out.println("Patient" + body_part_patient.toString());
		System.out.println("Staff" + body_part_staff.toString());
		System.out.println(body_part_staff==null);
		if(body_part_staff.isEmpty() || body_part_patient.isEmpty() || body_part_staff.containsAll(body_part_patient)) {
			// set the treatment time for the patient
			String updateTreatInPatientSession = "UPDATE PATIENT_SESSION SET TREATED='Y',TREATMENT_TIME=CURRENT_TIMESTAMP WHERE ID = "+pid;
			System.out.println("Patient is in Treatment Phase now.\n");
			ResultSet r = executeStringQuery(conn, updateTreatInPatientSession);
		}
		else {
			System.out.println("Inadequate Privilege");
		}
		EnterVitalMenu(conn,pid,staff_id);
	}
}