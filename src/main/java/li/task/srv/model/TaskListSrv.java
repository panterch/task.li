package li.task.srv.model;

public interface TaskListSrv {

	public abstract TaskList getTaskList(String id) throws Exception;

	public abstract TaskList storeTaskList(TaskList taskList) throws Exception;

}