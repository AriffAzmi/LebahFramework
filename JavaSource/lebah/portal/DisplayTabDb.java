/* ************************************************************************
LEBAH PORTAL FRAMEWORK
Copyright (C) 2007  Shamsul Bahrin

* ************************************************************************ */



package lebah.portal; 

import java.sql.*;
import java.util.*;

import lebah.db.Db;
import lebah.db.DbException;
import lebah.db.SQLRenderer;
import lebah.portal.element.Module2;
import lebah.portal.element.Tab;

/**
 * 
 * @author Shamsul Bahrin Abd Mutalib
  * @version 1.0
 */

public class DisplayTabDb {
	
	public static Tab getTab(String usrlogin, String tabid) throws DbException {
		Tab tab = null;
		Db db = null;
		String sql = "";
		try {
			db = new Db();
			Statement stmt = db.getStatement();
			SQLRenderer r = new SQLRenderer();
			r.add("tab_title");
			r.add("display_type");
			r.add("user_login", usrlogin);
			r.add("tab_id", tabid);
			sql = r.getSQLSelect("tab_user");
			
			ResultSet rs = stmt.executeQuery(sql);
			if ( rs.next() ) {
				String tab_title = rs.getString("tab_title");
				String displaytype = rs.getString("display_type");
				tab = new Tab(tabid, tab_title, displaytype);
			}
		} catch ( SQLException sqlex ) {
			throw new DbException( sqlex.getMessage() + " : " + sql);
		} finally {
			if ( db != null ) db.close();
		}
		return tab;
	}
	

	
	public static Vector getTabs(String usrlogin) throws DbException {
		Db db = null;
		String sql = "";
		try {
			db = new Db();
			Statement stmt = db.getStatement();
			Vector<Tab> v = new Vector<Tab>();
			SQLRenderer r = new SQLRenderer();

			//get user role
			String role = "";
			{
				sql =
				r
				.add("user_login", usrlogin)
				.add("user_role")
				.getSQLSelect("users")
				;
				ResultSet rs = db.getStatement().executeQuery(sql);
				if ( rs.next() ) role = rs.getString(1);
				
			}			
			
			//get display type from tab_template
			Hashtable<String, String> tabs = new Hashtable<String, String>();
			{
				
				r.reset();
				r.add("tab_id");
				r.add("display_type");
				r.add("user_login", role);
				sql = r.getSQLSelect("tab_template");
				ResultSet rs = stmt.executeQuery(sql);
				while ( rs.next()) {
					tabs.put(rs.getString(1), rs.getString(2));
				}
			}
			
			//get this user tabs
			{
			
				r.reset();
				r.add("tab_id");
				r.add("tab_title");
				r.add("display_type");
				r.add("sequence");
				r.add("locked");
				r.add("user_login", usrlogin);
				r.add("hide", 0);
				sql = r.getSQLSelect("tab_user", "sequence");
				
				ResultSet rs = stmt.executeQuery(sql);
				while ( rs.next() ) {
					Tab tab = new Tab();
					tab.setId(rs.getString("tab_id"));
					tab.setTitle(rs.getString("tab_title"));
					tab.setSequence(rs.getInt("sequence"));
					int locked = rs.getInt("locked");
					tab.setLocked(locked == 1 ? true : false);
					//get display type for locked tab
					if ( tab.isLocked() ) {
						tab.setDisplaytype(tabs.get(tab.getId()));
					} 
					else {
						tab.setDisplaytype(rs.getString("display_type"));
					}

					v.addElement(tab);
				}
			}
			for (Tab tab : v ) {
				if ( "pulldown_menu".equals(tab.getDisplayType())) {
					String module_table = tab.isLocked() ? "user_module_template t" : "user_module t";
					String usr = tab.isLocked() ? role: usrlogin;
					sql = r
					.reset()
					.add("t.tab_id", tab.getId())
					.add("t.user_login", usr)
					.add("t.module_id")
					.add("t.module_custom_title")
					.add("m.module_title")
					.add("m.module_class")
					.relate("t.module_id", "m.module_id")
					.getSQLSelect(module_table + ", module m").concat(" order by t.sequence")
					;
					ResultSet rs = stmt.executeQuery(sql);
					while ( rs.next()) {
						String moduleId = rs.getString("module_id");
						String moduleTitle = rs.getString("module_title");
						String moduleClass = rs.getString("module_class");
						String s = rs.getString("module_custom_title");
						Module2 module = new Module2(moduleId, moduleTitle, moduleClass, s);
						tab.addModule(module);
					}
				}
			}
			return v;
		} catch ( SQLException ex ) {
			throw new DbException(ex.getMessage() + ": " + sql);
		} finally {
			if ( db != null ) db.close();
		}
	}
	
    public static void saveTabs(String[] tabIds, String user) throws Exception {
    	if ( tabIds == null ) return;
    	
    	Db db = null;
    	String sql = "";
    	Connection conn = null;
    	try {
    		db = new Db();
    		conn = db.getConnection();
    		conn.setAutoCommit(false);
    		int seq = 0;
        	for ( String tabId : tabIds ) {
        		seq = seq + 1;
        		sql = "update tab_user set sequence = " + seq + " where tab_id = '" + tabId + "' and user_login = '" + user + "'";
        		db.getStatement().executeUpdate(sql);
        		//-
        	}
        	conn.commit();
    	} catch ( SQLException sqex ) {
			try {
				conn.rollback();
			} catch ( SQLException rollex ) {}
			throw sqex;
    	} finally {
    		if ( db != null ) db.close();
    	}
    }
    
	public static boolean addNewTab(String usrlogin, String tabtitle) throws DbException {
		return addNewTab(usrlogin, tabtitle, "");		
	}
	
	public static boolean addNewTab(String usrlogin, String tabtitle, String displaytype) throws DbException {
		if ( "".equals(displaytype) ) displaytype = "left_navigation";
		Db db = null;
		String sql = "";
		try {
			db = new Db();
			Statement stmt = db.getStatement();
			//make tabid as unique
			String tabid = lebah.db.UniqueID.getUID();

			//get max sequence number
			int max_seq = 0;			
			{
				sql = "SELECT MAX(sequence) AS seq FROM tab_user WHERE user_login = '" + usrlogin + "'";	
				ResultSet rs = stmt.executeQuery(sql);
				if ( rs.next() ) 	max_seq = rs.getInt("seq");
			}
				
			SQLRenderer r = new SQLRenderer();
			int seq = max_seq + 1;
			sql = add("tab_user", usrlogin, tabtitle, displaytype, stmt, tabid, seq, r);
			//sql = add("tabs", usrlogin, tabtitle, displaytype, stmt, tabid, seq, r);
		} catch ( SQLException sqlex ) {
			throw new DbException( sqlex.getMessage() + ": " + sql);
		} finally {
			if ( db != null ) db.close();
		}
		return true;
	}

	private static String add(String table, String usrlogin, String tabtitle, String displaytype, Statement stmt, String tabid, int seq, SQLRenderer r) throws SQLException {
		String sql;
		r.reset();
		r.add("tab_id", tabid);
		r.add("tab_title", tabtitle);
		r.add("user_login", usrlogin);
		r.add("display_type", displaytype);
		r.add("sequence", seq);
		sql = r.getSQLInsert(table);
		stmt.executeUpdate(sql);
		return sql;
	}
	
	public static void deleteTab(String usrlogin, String tabid) throws DbException {
		Db db = null;
		String sql = "";
		try {
			db = new Db();
			Statement stmt = db.getStatement();
			
			//get this user's role
			String role = "";
			sql = "SELECT user_role FROM users WHERE user_login = '" + usrlogin + "' ";
			ResultSet rs = stmt.executeQuery(sql);
			if ( rs.next() ) role = rs.getString(1);
			
			//get the sequence for this tab
			int sequence = 0;
			sql = "SELECT sequence FROM tab_user WHERE tab_id = '" + tabid + "' AND user_login = '" + usrlogin + "' ";		
			rs = stmt.executeQuery(sql);
			if ( rs.next() ) sequence = rs.getInt("sequence");
			
			//delete all its child first
			sql = "DELETE FROM user_module WHERE tab_id = '" + tabid + "' AND user_login = '" + usrlogin + "' ";		
			stmt.executeUpdate(sql);
			
			//was this tab defined?
			boolean isDefined = false;
			sql = "SELECT tab_id FROM tab_template WHERE tab_id = '" + tabid + "' ";
			rs = stmt.executeQuery(sql);
			if ( rs.next() ) isDefined = true;
			
			//delete the tab
			if ( !isDefined ){
				sql = "DELETE FROM tab_user WHERE tab_id = '" + tabid + "' AND user_login = '" + usrlogin + "' ";		
				stmt.executeUpdate(sql);
			} else {
				sql = "UPDATE tab_user SET hide = 1 WHERE tab_id = '" + tabid + "' AND user_login = '" + usrlogin + "' ";
			}
			
			//-
			
			//must renumber
			sql = "UPDATE tab_user SET sequence = sequence - 1 WHERE user_login = '" + usrlogin + "' AND sequence > " + sequence;		
			stmt.executeUpdate(sql);
			
			//delete the tab
			//sql = "DELETE FROM tabs WHERE tab_id = '" + tabid + "' AND user_login = '" + usrlogin + "' ";		
			//stmt.executeUpdate(sql);
			
			//must renumber
			//sql = "UPDATE tabs SET sequence = sequence - 1 WHERE user_login = '" + usrlogin + "' AND sequence > " + sequence;		
			//stmt.executeUpdate(sql);
			
			
		} catch ( SQLException sqlex ) {
			throw new DbException( sqlex.getMessage() + ": " + sql);
		} finally {
			if ( db != null ) db.close();
		}
	
	}
	

	 
	public static void changeTitle(String usrlogin, String tab, String title) throws DbException {
		Db db = null;
		String sql = "";
		try {
			db = new Db();
			Statement stmt = db.getStatement();
			sql = "UPDATE tab_user SET tab_title = '" + title + "' WHERE tab_id = '" + tab + "' AND user_login = '" + usrlogin + "'";	
			stmt.executeUpdate(sql);
			
			sql = "UPDATE tabs SET tab_title = '" + title + "' WHERE tab_id = '" + tab + "' AND user_login = '" + usrlogin + "'";	
			stmt.executeUpdate(sql);			
		} catch ( SQLException sqlex ) {
			throw new DbException( sqlex.getMessage() + ": " + sql);
		} finally {
			if ( db != null ) db.close();
		}			
			
	}
	
    public static void changeDisplayType(String usrlogin, String tab, String displaytype) throws DbException {
        Db db = null;
        String sql = "";
        try {
            db = new Db();
            Statement stmt = db.getStatement();
            sql = "UPDATE tab_user SET display_type = '" + displaytype + "' " +
            "WHERE tab_id = '" + tab + "' AND user_login = '" + usrlogin + "'"; 
            stmt.executeUpdate(sql);
            
            sql = "UPDATE tabs SET display_type = '" + displaytype + "' " +
            "WHERE tab_id = '" + tab + "' AND user_login = '" + usrlogin + "'"; 
            stmt.executeUpdate(sql);            
        } catch ( SQLException sqlex ) {
            throw new DbException( sqlex.getMessage() + ": " + sql);
        } finally {
            if ( db != null ) db.close();
        }           
            
    }	
	
	

}
