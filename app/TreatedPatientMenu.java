package app;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import oracle.jdbc.proxy.annotation.Pre;

public class TreatedPatientMenu extends Staff {
    private final int patientSessionID;
    private final String staffID;
    private int ref_facility_id = 0;
    private int ref_emp_id = 0;
    List<Integer> ref_reason_codes= new ArrayList<Integer>();
    HashMap<String,ArrayList<String>> ref_reason = new HashMap<String, ArrayList<String>>();
    private PreparedStatement insertNegExp;
    private PreparedStatement insertReason;
    private HashMap<String,String> negExp = new HashMap<String, String>();
    private HashMap<Integer,ArrayList<String>> ref_reasons_with_id = new HashMap<Integer, ArrayList<String>>();

    TreatedPatientMenu(String staffID, Integer patientSessionID) {
        super(staffID);
        this.staffID = staffID;
        this.patientSessionID = patientSessionID;
    }

    public void displayTreatedPatientMenu(Connection conn) throws Exception {
        // only medical staff has access
        String check_medical = "SELECT DESIGNATION FROM STAFF WHERE EMPLOYEE_ID = " + this.staffID;
        ResultSet medical_or_not = executeStringQuery(conn, check_medical);
        medical_or_not.next();
        if (medical_or_not.getString(1).equalsIgnoreCase("non-medical")) {
            System.out.println("Access Denied. Only Medical Staff has access. Going back!\n");
            StaffMenuDisplay(conn);
        }
        Scanner scan = new Scanner(System.in);
        System.out.println("----------------------------TREATED PATIENT MENU ----------------------------");
        System.out.println("1. Checkout");
        System.out.println("2. Go Back");
        int select = scan.nextInt();
        scan.nextLine();
        if (select == 1) {
            displayStaffPatientCheckout(conn,null,null,0,0);
        } else if (select == 2) {
            StaffMenuDisplay(conn);
        } else {
            System.out.println("Please enter a valid input:");
            displayTreatedPatientMenu(conn);
        }
    }

    void displayStaffPatientCheckout(Connection conn,String dischargeStatus,String treatmentDesc, Integer referralStatusID, Integer negExpCode) throws Exception {
        Scanner scan = new Scanner(System.in);
//        String reason = null;
//        int addReportDetails = 0;
        int select = 0;
        do {
            System.out.println("----------------------------STAFF-PATIENT REPORT MENU --------------------------");
            System.out.println("1. Discharge Status");
            System.out.println("2. Referral Status");
            System.out.println("3. Treatment");
            System.out.println("4. Negative Experience");
            System.out.println("5. Go back");
            System.out.println("6. Submit");
            select = scan.nextInt();
            scan.nextLine();
            if (select == 1) {
                dischargeStatus = displayDischargeStatus(conn,treatmentDesc,referralStatusID, negExpCode);
                System.out.println(dischargeStatus);
            }
            if (select == 2) {
                if (dischargeStatus == null) {
                    System.out.println("Enter discharge status first\n\n");
                } else if (dischargeStatus.toLowerCase().equals("referred")) {
                	int facilityID = 0;
                    int referrerID = 0;
                    displayReferralStatus(conn,facilityID,referrerID,dischargeStatus,treatmentDesc,referralStatusID,negExpCode);
                } else {
                    System.out.println("Can NOT enter Referral is the Discharge Status is not Referred.");
                }
            }
            if (select == 3) {
                System.out.println("Enter treatment description:");
                treatmentDesc = scan.nextLine();
            }
            if (select == 4) {
                negExpCode = displayNegativeExperience(conn);
            }
            if (select == 5) {
                displayTreatedPatientMenu(conn);
            }
            if (select == 6) {
            	//System.out.println(dischargeStatus);
            	//System.out.println(treatmentDesc);
            	if(dischargeStatus == null || treatmentDesc==null) {
            		System.out.println("Enter discharge status and treatment description. Its mandatory.");
            	}
            	else if ((dischargeStatus!=null && dischargeStatus.toLowerCase().equals("referred") && referralStatusID == 0)) {
                    System.out.println("Enter the Refferal Status");
                } else if(dischargeStatus!=null && dischargeStatus.toLowerCase().equals("referred") && referralStatusID!=0){
                	if(this.ref_emp_id!=0 && this.ref_facility_id!=0 && this.ref_reason_codes!=null) {
	                	displayReportConfirmation(conn, referralStatusID, negExpCode, dischargeStatus, treatmentDesc);
                	}
                	break;
                } else {
                	displayReportConfirmation(conn, referralStatusID, negExpCode, dischargeStatus, treatmentDesc);
                	break;
                }
            }
        } while (select <=6 && select >=1);
        
    }

    String displayDischargeStatus(Connection conn,String treatmentDesc, Integer referralStatusID, Integer negExpCode) throws Exception {
        Scanner scan = new Scanner(System.in);
        System.out.println("1. Successful Treatment");
        System.out.println("2. Deceased");
        System.out.println("3. Referred");
        System.out.println("4. Go back");
        int select = scan.nextInt();
        scan.nextLine();
        String dischargeStatusValue = null;
        if (select >= 1 && select < 4) {
            switch (select) {
                case 1:
                    dischargeStatusValue = "Successful Treatment";
                    break;
                case 2:
                    dischargeStatusValue = "Deceased";
                    break;
                case 3:
                    dischargeStatusValue = "Referred";
                    break;

            }
        } else if (select == 4) {
            displayStaffPatientCheckout(conn,dischargeStatusValue,treatmentDesc,referralStatusID, negExpCode);
        } else {
            System.out.println("Please enter a valid input.");
            displayDischargeStatus(conn,treatmentDesc,referralStatusID, negExpCode);
        }
        return dischargeStatusValue;
    }

    void displayReferralStatus(Connection conn, int facilityID,int referrerID,String dischargeStatus,String treatmentDesc,Integer referralStatusID, Integer negExpCode) throws Exception {
        Scanner scan = new Scanner(System.in);
        System.out.println("1. Facility ID");
        System.out.println("2. Referrer ID");
        System.out.println("3. Add reason");
        System.out.println("4. Go back");
        int select = scan.nextInt();
        int referralStatusId = 0;
        scan.nextLine();
        List<Integer> referralReasonCode = new ArrayList<Integer>();
        while(select<=4 && select>=1) {
	        if (select == 1) {
	            PreparedStatement stmt = conn.prepareStatement("SELECT NAME,FACILITY_ID FROM HOSPITAL WHERE FACILITY_ID NOT IN (SELECT FACILITY_ID FROM STAFF WHERE EMPLOYEE_ID = " + this.staffID + ")");
	            ResultSet rs1 = stmt.executeQuery();
	            HashMap<Integer, String> facilities = new HashMap<Integer, String>();
	            while (rs1.next()) {
	                facilities.put(rs1.getInt("FACILITY_ID"), rs1.getString("NAME"));
	            }
	            System.out.println("Facilities available (ID: NAME):");
	            for (Map.Entry<Integer, String> map : facilities.entrySet()) {
	                System.out.println(map.getKey() + ": " + map.getValue());
	            }
	            System.out.println("Enter the Facility ID:");
	            facilityID = scan.nextInt();
	            scan.nextLine();
	            PreparedStatement getFacilityID = conn.prepareStatement("select facility_id from staff where employee_id = ?");
	            getFacilityID.setString(1, this.staffID);
	            ResultSet rs = getFacilityID.executeQuery();
	            int selfFacilityID = 0;
	            while (rs.next()) {
	                selfFacilityID = rs.getInt("FACILITY_ID");
	            }
	            if (selfFacilityID == facilityID) {
	                System.out.println("Can NOT refer to the Facility you are employed at. Please enter another Facility ID:");
	                facilityID = scan.nextInt();
	                scan.nextLine();
	            }
	        } else if (select == 2) {
	            System.out.println("Enter the Referrer ID:");
	            referrerID = scan.nextInt();
	            scan.nextLine();
	            //displayReferralReason(conn);
	        } else if (select == 3) {
	        	this.ref_emp_id = referrerID;
	            this.ref_facility_id = facilityID;
	            referralReasonCode = displayReferralReason(conn);
	            this.ref_reason_codes = referralReasonCode;
	            referralStatusId = 1;
	        } else if(select == 4) {
	        	displayStaffPatientCheckout(conn,dischargeStatus,treatmentDesc,referralStatusId, negExpCode);
	        	break;
	        }
	        System.out.println("1. Facility ID");
	        System.out.println("2. Referrer ID");
	        System.out.println("3. Add reason");
	        System.out.println("4. Go back");
	        select = scan.nextInt();
        }
    }
    
    
    
    int addReferralReasons(Connection conn, List<Integer> referralReasonCode,int facilityID,int referrerID) throws Exception {
    	 String insert_rstatus = "insert into referral_status (facility_id, employee_id) " +
                 "values ("+facilityID+" , '" +referrerID+ "')";
         System.out.println(insert_rstatus);
         PreparedStatement insertReferralStatus = conn.prepareStatement(insert_rstatus);
         insertReferralStatus.executeQuery();
         int refStatusID = 0;
         PreparedStatement getRefStatSeqID = conn.prepareStatement("select referral_status_id_seq.currval from dual");
         ResultSet rs3 = getRefStatSeqID.executeQuery();
         while (rs3.next()) {
             refStatusID = rs3.getInt("CURRVAL");
         }
         int count = 1;
         for (Integer x : referralReasonCode) {
        	 x = x+count;
        	 String refReasonMapping = "insert into refferal_reason_mapping(REFFERAL_STATUS_ID, REASON_CODE_ID) values("+refStatusID +", "+ x +")";
        	 //System.out.println(refReasonMapping);
             PreparedStatement insertRefStatusReasonMapping = conn.prepareStatement(refReasonMapping);
             insertRefStatusReasonMapping.executeQuery();
             count += 1;
         }
         return refStatusID;
    }

    List<Integer> displayReferralReason(Connection conn) throws Exception {
        Scanner scan = new Scanner(System.in);
        List<Integer> reasonIDs = new ArrayList<Integer>();
        System.out.println("1. Reason");
        System.out.println("2. Go back");
        int select = scan.nextInt();
        scan.nextLine();
        int reasonCount = 0;
        while (reasonCount < 4 && select == 1) {
            System.out.println("Following are the services available:");
            //List<String> services = new ArrayList<String>();
            HashMap<Integer,String> services = new HashMap<Integer, String>();
            PreparedStatement getServices = conn.prepareStatement("select sd.name from hospital_service_dept_mapping hsdm inner join service_dept sd on hsdm.service_dept_id = sd.id where facility_id in ?");
            getServices.setInt(1, this.ref_facility_id);
            ResultSet rsServ = getServices.executeQuery();
            int i = 1;
            while (rsServ.next()) {
                services.put(i,rsServ.getString("NAME"));
                i++;
            }

            System.out.println("Please choose a service to refer: " + services);
            int service = scan.nextInt();

            System.out.println("Enter a reason code:");
            System.out.println("1. Service unavailable at time of visit");
            System.out.println("2. Service not present at facility");
            System.out.println("3. Non-payment");
            int selectRsn = scan.nextInt();
            String referralReason = scan.nextLine();

            if (selectRsn >= 1 && selectRsn < 4) {
                switch (selectRsn) {
                    case 1:
                        referralReason = "Service unavailable at time of visit";
                        break;
                    case 2:
                        referralReason = "Service not present at facility";
                        break;
                    case 3:
                        referralReason = "Non-payment";
                        break;
                }
            }

            System.out.println("Enter a decription:");
            String description = scan.nextLine();
            this.ref_reason.put(referralReason, new ArrayList<String>(List.of(services.get(service),description)));
            this.ref_reasons_with_id.put(selectRsn,new ArrayList<String>(List.of(services.get(service),description)));
            
            int reasonID = 0;
            try {
	            PreparedStatement getAddSeqID = conn.prepareStatement("select reason_id_seq.currval from dual");
	            ResultSet rs3 = getAddSeqID.executeQuery();
	            while (rs3.next()) {
	                reasonID = rs3.getInt("CURRVAL");
	            }
	            reasonIDs.add(reasonID);
            } catch (Exception e) {
            	PreparedStatement getnextval = conn.prepareStatement("select reason_id_seq.nextval from dual");
                ResultSet rs4 = getnextval.executeQuery();
                PreparedStatement getAddSeqID = conn.prepareStatement("select reason_id_seq.currval from dual");
                ResultSet rs3 = getAddSeqID.executeQuery();
	             while (rs3.next()) {
	                 reasonID = rs3.getInt("CURRVAL");
	             }
	             reasonIDs.add(reasonID);
			}
            System.out.println("Do you wish to enter more reasons?");
            System.out.println("1. Reason");
            System.out.println("2. Go back");
            select = scan.nextInt();
            scan.nextLine();
            reasonCount++;
        }
        return reasonIDs;
    }

    Integer displayNegativeExperience(Connection conn) throws SQLException {
        Scanner scan = new Scanner(System.in);
        System.out.println("1. Negative Experience Code");
        System.out.println("2. Go back");
        int select = scan.nextInt();
        scan.nextLine();
        if (select == 1) {
            System.out.println("Please select a reason (Number):");
            System.out.println("1. Misdiagnosis");
            System.out.println("2. Patient acquired an infection during hospital stay");
            int neg_exp_code = scan.nextInt();
            scan.nextLine();
            String neg_exp = neg_exp_code == 1 ? "Misdiagnosis" : "Infected_at_hospital";
            System.out.println("Please enter a text description for the chosen reason:");
            String desc = scan.nextLine();
            this.negExp.put(neg_exp,desc);
            this.insertNegExp = conn.prepareStatement("INSERT INTO NEGATIVE_EXP (CODE, DESCRIPTION) VALUES (?, ?)");
            this.insertNegExp.setString(1, neg_exp);
            this.insertNegExp.setString(2, desc);
            //insertNegExp.executeQuery();
            int negExpSeqID = 0;
            try {
                PreparedStatement getAddSeqID = conn.prepareStatement("select neg_exp_id_seq.currval from dual");
                ResultSet rs3 = getAddSeqID.executeQuery();
                while (rs3.next()) {
                    negExpSeqID = rs3.getInt("CURRVAL");
                }
            } catch (SQLException s) {
                PreparedStatement getnext = conn.prepareStatement("select neg_exp_id_seq.nextval from dual");
                ResultSet rs4 = getnext.executeQuery();
                PreparedStatement getAddSeqID = conn.prepareStatement("select neg_exp_id_seq.currval from dual");
                ResultSet rs3 = getAddSeqID.executeQuery();
                while (rs3.next()) {
                    negExpSeqID = rs3.getInt("CURRVAL");
                }
            }
            return negExpSeqID;
        } else if (select == 2) {
            return 0;
        } else {
            System.out.println("Please enter a valid input.");
            displayNegativeExperience(conn);
        }
        return 0;
    }

    void displayReportConfirmation(Connection conn, Integer referralStatusID, Integer negExpCode, String dischargeStatus, String treatmentDesc) throws Exception {
        Scanner scan = new Scanner(System.in);
        System.out.println("-----------Report--------------");
        System.out.println("Discharge status: " + dischargeStatus);
        if(referralStatusID == 1) {
        	System.out.println("Facility referred to: "+this.ref_facility_id);
        	System.out.println("Referrer emp id: " + this.ref_emp_id);
        	System.out.println("referral reason: " + this.ref_reason);
        }
        System.out.println("Treatement description: " + treatmentDesc);
        if(negExpCode!=0) {
        	System.out.println("Neg experience reason:" + this.negExp);
        }
        System.out.println("1. Confirm");
        System.out.println("2. Go back");
        int select = scan.nextInt();
        scan.nextLine();
        if (select == 1) {
        	// referred
        	if(referralStatusID == 1) {
        		for(Map.Entry entry: this.ref_reasons_with_id.entrySet()) {
        			ArrayList<String> val = (ArrayList<String>) entry.getValue();
        			String enter_in_reason = "insert into reason(reason_code,service_name,description) values("+entry.getKey()+", '"+val.get(0)+"', '"+val.get(1)+"')";
        			executeStringQuery(conn, enter_in_reason);
        		}
        		System.out.println("inserted in reason");
        	}
        	// reffered and neg exp
        	if(negExpCode!=0 && referralStatusID == 1) {
        		negExpCode +=1;
        		this.insertNegExp.executeQuery();
        		int ref_status_id = addReferralReasons(conn, this.ref_reason_codes, this.ref_facility_id, this.ref_emp_id);
        		String insertReport = "insert into report (patient_id, referral_status_id, negative_exp_id, discharge_status, treatment_given) " +
                        "values ("+ this.patientSessionID +", "+ ref_status_id +", " +negExpCode +", '" + dischargeStatus+"', '"+treatmentDesc +"')";
        		//System.out.println(insertReport);
        		 PreparedStatement insertPatientSym = conn.prepareStatement(insertReport);
                 insertPatientSym.executeQuery();
                 System.out.println("Check-out process completed.");
        	}
        	// reffered and no neg exp
        	if(referralStatusID==1 && negExpCode == 0) {
        		int ref_status_id = addReferralReasons(conn, this.ref_reason_codes, this.ref_facility_id, this.ref_emp_id);
        		String insertReport = "insert into report (patient_id, referral_status_id, discharge_status, treatment_given) " +
                        "values ("+ this.patientSessionID +", "+ ref_status_id +", '" + dischargeStatus+"', '"+treatmentDesc +"')";
        		 PreparedStatement insertPatientSym = conn.prepareStatement(insertReport);
                 insertPatientSym.executeQuery();
                 System.out.println("Check-out process completed.");
        	}
        	// not reffered no neg exp
        	if(referralStatusID!=1 && negExpCode == 0) {
        		String insertReport = "insert into report (patient_id, discharge_status, treatment_given) " +
                        "values ("+ this.patientSessionID + ", '" + dischargeStatus+"', '"+treatmentDesc +"')";
        		//System.out.println(insertReport);
        		PreparedStatement insertPatientSym = conn.prepareStatement(insertReport);
                insertPatientSym.executeQuery();
                System.out.println("Check-out process completed.");
        	}
        	// not refferef neg exp
        	if(referralStatusID!=1 && negExpCode!=0) {
        		negExpCode +=1;
        		this.insertNegExp.executeQuery();
        		String insertReport = "insert into report (patient_id, discharge_status, negative_exp_id, treatment_given) " +
                        "values ("+ this.patientSessionID + ", '" + dischargeStatus+"', "+negExpCode+", '"+treatmentDesc +"')";
        		//System.out.println(insertReport);
        		PreparedStatement insertPatientSym = conn.prepareStatement(insertReport);
                insertPatientSym.executeQuery();
                System.out.println("Check-out process completed.");
        	}
           
            StaffMenuDisplay(conn);
        } else if (select == 2) {
            displayStaffPatientCheckout(conn,dischargeStatus,treatmentDesc,referralStatusID,negExpCode);
        } else {
            System.out.println("Please enter a valid input.");
            displayReportConfirmation(conn, referralStatusID, negExpCode, dischargeStatus, treatmentDesc);
        }
    }
}