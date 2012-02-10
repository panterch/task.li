/**
 * 
 */
package li.task.srv.model;

import java.util.HashMap;
import java.util.Map;

/**
 * @author seb
 * 
 */
public class InMemoryTaskListSrv implements TaskListSrv {

	private Map<String, TaskList> store = new HashMap<String, TaskList>();
	
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see li.task.srv.model.TaskListSrv#getTaskList(java.lang.String)
	 */
	@Override
	public TaskList getTaskList(String id) {
		return store.get(id);
	}


	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * li.task.srv.model.TaskListSrv#storeTaskList(li.task.srv.model.TaskList)
	 */
	@Override
	public TaskList storeTaskList(TaskList taskList) throws Exception {
		this.store.put(taskList.getId(), taskList);
		return taskList;
	}


	public Map<String, TaskList> getStore() {
		return store;
	}


	public void setStore(Map<String, TaskList> store) {
		this.store = store;
	}
	
	
}
