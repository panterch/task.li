package li.task.srv.mail;

import java.io.File;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.springframework.core.io.ClassPathResource;

public class SimpleMaildirTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();
	private String workDir = null;

	@Before
	public void setUp() throws Exception {
		this.workDir = folder.newFolder().getAbsolutePath();
		ClassPathResource maildirArchive = new ClassPathResource(
				"simple-maildir.tar");
		String archive = maildirArchive.getFile().getAbsolutePath();
		Runtime.getRuntime().exec("tar xvf " + archive + " -C " + workDir);
	}

	@Test
	public void shouldExtractFixtureArchive() throws Exception {
		System.out.println(workDir);
		Assert.assertTrue(new File(workDir, "Maildir").isDirectory());
	}
	

}
