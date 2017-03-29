/* ************************************************************************
LEBAH PORTAL FRAMEWORK, http://lebah.sf.net
Copyright (C) 2007  Shamsul Bahrin







but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.

* ************************************************************************ */

package lebah.portal.velocity;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Properties;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import lebah.portal.handler.DeviceHandler;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;

/**
 * @author Shamsul Bahrin Abd Mutalib
 * @version 1.01
 */
public abstract class VServlet extends HttpServlet {
	protected VelocityEngine engine;
	protected VelocityContext context;
	
    public void init( ServletConfig config ) throws ServletException {
        super.init(config);
    }
    
    /*
     * As of 2009-08-13, this method will be called from the DesktopController
     * to initialize the Velocity Engine and Velocity Context.
     */
    public void initVelocity(ServletConfig config) throws ServletException {
        try { 
        	engine = new VelocityEngine();
        	context = new VelocityContext();
	        Properties p = loadConfiguration(config);
	        engine.setApplicationAttribute("javax.servlet.ServletContext", config.getServletContext());
        	engine.init(p);
        } catch ( Exception e ) {
	        System.out.println( e.getMessage() );
        }
    }

	private Properties loadConfiguration(ServletConfig config ) throws IOException, FileNotFoundException {
		/*
        *  get our properties file and load it
        */

		String propsFile = config.getInitParameter("properties");
		Properties p = new Properties();
        if ( propsFile != null ) {
        	
        	System.out.println("GETTING VELOCITY PROPERTIIES FILE.....");
        	System.out.println(propsFile);
        	
			String realPath = getServletContext().getRealPath(propsFile);
            if ( realPath != null ) propsFile = realPath;
            p.load( new FileInputStream(propsFile) );
        }
		/*
        *  first, normalize our velocity log file to be in the 
        *  webapp
        */
		String log = p.getProperty( Velocity.RUNTIME_LOG);
		if (log != null ) {
            log = getServletContext().getRealPath( log );
			if (log != null) 
				p.setProperty( Velocity.RUNTIME_LOG, log );
        }
        /*
         *  now, if there is a file loader resource path, treat it the
         *  same way.
         */
        String path = p.getProperty( Velocity.FILE_RESOURCE_LOADER_PATH );
        if ( path != null) {
        	System.out.println("resource loader path = " + path);
            path = getServletContext().getRealPath(  path );
            if ( path != null) {
                p.setProperty( Velocity.FILE_RESOURCE_LOADER_PATH, path );
            }
        }
        
        //IN DEPLOYMENT ENVIRONMENT MUST REMOVE THIS COMMENT
        p.setProperty( Velocity.FILE_RESOURCE_LOADER_CACHE, "true" );
        
        
       System.out.println("Resource Loader = " +  p.getProperty(RuntimeConstants.RESOURCE_LOADER));
        
        return p;
    }	
    
	/*
    private Properties loadConfigurationSimple(ServletConfig config ) throws IOException, FileNotFoundException {
   		String path = config.getServletContext().getRealPath("/");
        Properties p = new Properties();
        p.setProperty( Velocity.FILE_RESOURCE_LOADER_PATH, path );
        p.setProperty( Velocity.FILE_RESOURCE_LOADER_CACHE, "true" );
        //p.setProperty( "runtime.log", path + "velocity.log" );  
        return p;	    	
    }
    */

}
