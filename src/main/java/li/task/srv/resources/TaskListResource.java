package li.task.srv.resources;

import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.Path;

import li.task.srv.model.Task;
import li.task.srv.model.TaskList;

@Path("/tasklist")
public class TaskListResource {

    @GET 
    @Produces("application/json")
    public TaskList getTaskList() {
    	TaskList taskList = new TaskList(1);
    	taskList.addTask(new Task("Buy milk"));
    	taskList.addTask(new Task("Buy salt"));
        return taskList;
    }
}