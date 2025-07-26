import java.sql.*;
import java.util.Scanner;

public class Library {

	static final String JDBC_URL = "jdbc:mysql://localhost:3306/library_db";
	static final String DB_USER = "root";
	static final String DB_PASSWORD = "ThaboJunior@1348"; 

	public static void main(String[] args) {

		try (Connection conn = DriverManager.getConnection(JDBC_URL, DB_USER, DB_PASSWORD);
				Scanner scanner = new Scanner(System.in)) {

			System.out.println("Database Connection Successful!");

			while (true) {
				System.out.println("\n--- Library Management System ---\n");
				System.out.println("1. Add new book");
				System.out.println("2. View all books");
				System.out.println("3. Add new user");
				System.out.println("4. View all users");
				System.out.println("5. Borrow book");
				System.out.println("6. Return book");
				System.out.println("7. View transactions");
				System.out.println("0. Exit");
				System.out.print("Enter choice: ");
				int choice = Integer.parseInt(scanner.nextLine());

				System.out.println("");

				
				switch (choice) {
				case 1:
					addNewBook(conn, scanner);
					break;
				case 2:
					viewAllBooks(conn);
					break;
				case 3:
					addNewUser(conn, scanner);
					break;
				case 4:
					viewAllUsers(conn);
					break;
				case 5:
					borrowBook(conn, scanner);
					break;
				case 6:
					returnBook(conn, scanner);
					break;
				case 7:
					viewTransactions(conn);
					break;
				case 0:
					System.exit(0);
				default:
					System.out.println("Invalid choice.");
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	static void addNewBook(Connection conn, Scanner scanner) throws SQLException {
		
		System.out.print("Enter title: ");
		String title = scanner.nextLine();
		
		System.out.print("Enter author: ");
		String author = scanner.nextLine();

		String sql = "INSERT INTO books (title, author) VALUES (?, ?)";
		try (PreparedStatement stmt = conn.prepareStatement(sql)) {
			stmt.setString(1, title);
			stmt.setString(2, author);
			stmt.executeUpdate();
			System.out.println("New Book added!");
		}
	}

	static void viewAllBooks(Connection conn) throws SQLException {
		
		String sql = "SELECT * FROM books";
		try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
			System.out.println("Books:");
			while (rs.next()) {
				System.out.printf("ID: %d \n Title: %s \n Author: %s \n Available: %s\n", rs.getInt("id"),
						rs.getString("title"), rs.getString("author"), rs.getBoolean("available"));
				System.out.println("");
			}
		}
	}

	static void addNewUser(Connection conn, Scanner scanner) throws SQLException {
		
		System.out.print("Enter user name: ");
		String name = scanner.nextLine();

		String sql = "INSERT INTO users (name) VALUES (?)";
		try (PreparedStatement stmt = conn.prepareStatement(sql)) {
			stmt.setString(1, name);
			stmt.executeUpdate();
			System.out.println("User added!");
		}
	}

	static void viewAllUsers(Connection conn) throws SQLException {
		
		String sql = "SELECT * FROM users";
		try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
			System.out.println("Users:");
			while (rs.next()) {
				System.out.printf("ID: %d \n Name: %s\n", rs.getInt("id"), rs.getString("name"));
				System.out.println("");
			}
		}
	}

	static void borrowBook(Connection conn, Scanner scanner) throws SQLException {
		
		System.out.print("Enter user ID: ");
		int userId = Integer.parseInt(scanner.nextLine());
		
		System.out.print("Enter book ID: ");
		int bookId = Integer.parseInt(scanner.nextLine());

		// Check if book is available
		String checkSql = "SELECT available FROM books WHERE id = ?";
		try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
			checkStmt.setInt(1, bookId);
			ResultSet rs = checkStmt.executeQuery();
			if (rs.next() && rs.getBoolean("available")) {
				// Mark book as unavailable
				String updateSql = "UPDATE books SET available = FALSE WHERE id = ?";
				try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
					updateStmt.setInt(1, bookId);
					updateStmt.executeUpdate();
				}

				// Add transaction
				String transSql = "INSERT INTO transactions (user_id, book_id, action) VALUES (?, ?, 'BORROW')";
				try (PreparedStatement transStmt = conn.prepareStatement(transSql)) {
					transStmt.setInt(1, userId);
					transStmt.setInt(2, bookId);
					transStmt.executeUpdate();
				}

				System.out.println("Book borrowed!");
			} else {
				System.out.println("Book is not available.");
			}
		}
	}

	static void returnBook(Connection conn, Scanner scanner) throws SQLException {
		
    System.out.print("Enter user ID: ");
    int userId = Integer.parseInt(scanner.nextLine());

    System.out.print("Enter book ID: ");
    int bookId = Integer.parseInt(scanner.nextLine());

    // Step 1: Check if book is currently borrowed
    String checkSql = "SELECT available FROM books WHERE id = ?";
    try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
        checkStmt.setInt(1, bookId);
        try (ResultSet rs = checkStmt.executeQuery()) {
            if (rs.next()) {
                boolean available = rs.getBoolean("available");
                if (available) {
                    // Book is already available, can't return
                    System.out.println("Error: This book was not borrowed, so it can't be returned.");
                    return; 
                    
                    //if book is unavailable (was borrowed)
                } else {
                    // Step 2: Mark book as available
                    String updateSql = "UPDATE books SET available = TRUE WHERE id = ?";
                    try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                        updateStmt.setInt(1, bookId);
                        updateStmt.executeUpdate();
                    }

                    // Step 3: Add return transaction
                    String transSql = "INSERT INTO transactions (user_id, book_id, action) VALUES (?, ?, 'RETURN')";
                    try (PreparedStatement transStmt = conn.prepareStatement(transSql)) {
                        transStmt.setInt(1, userId);
                        transStmt.setInt(2, bookId);
                        transStmt.executeUpdate();
                    }

                    System.out.println("Book returned successfully!");
                }
            } else {
                // Book ID doesn't exist
                System.out.println("Error: Book not found.");
            }
        }
    }
}


	static void viewTransactions(Connection conn) throws SQLException {
		
	    String sql = "SELECT t.id, u.name, b.title, t.action, t.date "
	               + "FROM transactions t JOIN users u ON t.user_id = u.id "
	               + "JOIN books b ON t.book_id = b.id ORDER BY t.date DESC";
	    
	    try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
	        System.out.println("Transactions:");

	        boolean hasTransactions = false; // to check if any transactions exist

	        while (rs.next()) {
	            hasTransactions = true;
	            System.out.printf("ID: %d \n User: %s \n Book: %s \n Action: %s \n Date: %s\n",
	                    rs.getInt("id"),
	                    rs.getString("name"),
	                    rs.getString("title"),
	                    rs.getString("action"),
	                    rs.getTimestamp("date"));
	            System.out.println("");
	        }

	        if (!hasTransactions) {
	            System.out.println("No transactions made.");
	        }
	    }
	}

}

//test change