package app;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Scanner;

public class TreatedPatientMenu {
	public void displayTreatedPatientMenu(Connection conn) {
		 System.out.println("----------------------------TREATED PATIENT MENU --------------------------");
	     System.out.println("1. Checkout");
	     System.out.println("2. Go Back");
	}
}