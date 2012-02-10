package li.task.srv.resource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import li.task.srv.model.Task;
import li.task.srv.model.TaskList;
import li.task.srv.model.TaskListSrv;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sun.jersey.api.NotFoundException;

@Component
@Path("/")
public class TaskListResource {
	
	@Autowired TaskListSrv taskListSrv;

    @GET
    @Path("tasklists/{id}")
    @Produces("application/json")
    public TaskList getTaskList(@PathParam("id") String id) throws Exception {
    	TaskList taskList = taskListSrv.getTaskList(id);
    	if (null == taskList) { throw new NotFoundException(); }
        return taskList;
    }

    @GET
    @Path("_configuration")
    @Produces("text/plain")
    public String printConfiguration() {
    	StringBuilder configuration = new StringBuilder();
    	configuration.append("\nWired to taskListSrv property: ");
    	configuration.append(this.taskListSrv.toString());
        return configuration.toString();
    }

}