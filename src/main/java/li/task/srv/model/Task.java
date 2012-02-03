/**
 * 
 */
package li.task.srv.model;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * @author seb
 *
 */
@XmlRootElement
public class Task {
	
	private String name;

	public Task() {
	}
	
	public Task(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

}
