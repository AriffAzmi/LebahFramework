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
package lebah.repository;

/**
 * @author Shamsul Bahrin Abd Mutalib
 * @version 1.01
 */
public class Category {

	String id, name, type;
	
	public void setId(String s) {
		id = s;
	}
	
	public void setName(String s) {
		name = s;
	}
	
	public void setType(String s) {
		type = s;
	}
	
	public String getId() {
		return id;
	}
	
	
	public String getName() {
		return name;
	}
	
	public String getType() {
		return type;
	}
	

}
