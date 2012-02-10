/**
 * 
 */
package li.task.srv.model;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * @author seb
 *
 */
@XmlRootElement
public class Task implements Serializable{
	
	private static final long serialVersionUID = 2240411649690612112L;
	
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
