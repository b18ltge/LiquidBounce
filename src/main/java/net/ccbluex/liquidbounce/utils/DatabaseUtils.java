/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils;

import java.sql.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DatabaseUtils {
	
	private static final Logger logger = LogManager.getLogger("LiquidBounce");
	
	private static final byte[] DATABASE_URL1 = new byte[]{0x6a, 0x64, 0x62, 0x63, 0x3a, 0x6d, 0x79, 0x73, 0x71, 0x6c, 0x3a, 0x2f, 0x2f, 0x73, 0x71, 0x6c, 0x2e, 0x66, 0x72, 0x65, 0x65, 0x64, 0x62, 0x2e, 0x74, 0x65, 0x63, 0x68, 0x3a, 0x33, 0x33, 0x30, 0x36, 0x2f, 0x66, 0x72, 0x65, 0x65, 0x64, 0x62, 0x5f, 0x6c, 0x69, 0x71, 0x75, 0x69, 0x64, 0x62, 0x6f, 0x75, 0x6e, 0x63, 0x65};
    private static final byte[] DATABASE_USER1 = new byte[]{0x66, 0x72, 0x65, 0x65, 0x64, 0x62, 0x5f, 0x75, 0x73, 0x65, 0x72, 0x34};
	private static final byte[] DATABASE_PASSWORD1 = new byte[]{0x2a, 0x59, 0x37, 0x35, 0x62, 0x7a, 0x4e, 0x38, 0x70, 0x37, 0x2a, 0x71, 0x54, 0x67, 0x6d};
	
	
	private static final byte[] DATABASE_URL2 = new byte[]{0x6a, 0x64, 0x62, 0x63, 0x3a, 0x6d, 0x79, 0x73, 0x71, 0x6c, 0x3a, 0x2f, 0x2f, 0x73, 0x71, 0x6c, 0x2e, 0x66, 0x72, 0x65, 0x65, 0x64, 0x62, 0x2e, 0x74, 0x65, 0x63, 0x68, 0x3a, 0x33, 0x33, 0x30, 0x36, 0x2f, 0x66, 0x72, 0x65, 0x65, 0x64, 0x62, 0x5f, 0x62, 0x61, 0x63, 0x6b, 0x75, 0x70, 0x5f, 0x6c, 0x69, 0x71};
    private static final byte[] DATABASE_USER2 = new byte[]{0x66, 0x72, 0x65, 0x65, 0x64, 0x62, 0x5f, 0x61, 0x6e, 0x6b, 0x69, 0x6c, 0x61};
	private static final byte[] DATABASE_PASSWORD2 = new byte[]{0x24, 0x6e, 0x26, 0x73, 0x77, 0x25, 0x34, 0x77, 0x3f, 0x4a, 0x5a, 0x37, 0x79, 0x52, 0x73};

	
	private static Connection getConnection() {
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(new String(DATABASE_URL1), new String(DATABASE_USER1), new String(DATABASE_PASSWORD1));
        } catch (SQLException e) {
			logger.error("Couldn't connect to a database! (1)");
		}
		
		try {
            conn = DriverManager.getConnection(new String(DATABASE_URL2), new String(DATABASE_USER2), new String(DATABASE_PASSWORD2));
        } catch (SQLException e) {
			logger.error("Couldn't connect to a database! (2)");
		}

        return conn;
    }

	public static boolean containsUser(final String macString) {
        final Connection connection = getConnection();
		
		if (connection == null) {
			logger.error("Connection is null!");
			return false;
		}
		
        final String sql = "SELECT * FROM users where mac = ?";

        try ( final PreparedStatement statement = connection.prepareStatement(sql)) {
            connection.setAutoCommit(false);
            statement.setString(1, macString);
            final ResultSet resultSet = statement.executeQuery();
            connection.commit();

			final boolean result = resultSet.next();

            resultSet.close();
            return result;
        } catch (Exception exception) {
                try {
					logger.error("Transaction is being rolled back");
                    connection.rollback();
                } catch (SQLException ex) {
					logger.error(ex.getMessage());
                }
        }
        return false;
    }
}
