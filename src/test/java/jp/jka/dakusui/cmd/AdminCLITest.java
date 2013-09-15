package jp.jka.dakusui.cmd;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.github.dakusui.cmd.CommandResult;
import com.github.dakusui.cmd.CommandFactory;

public class AdminCLITest {

	@Test
	public void test() throws Exception {
		CommandResult result = 
				CommandFactory.runRemote(
						"ngsuser", 
						"testenv101", 
						"/home/ukaihiroshi01/.ssh/id_rsa.gsp-20" , 
						"cd /home/ngsuser &&  export JAVA_HOME=/usr/lib/jvm/ngs-jdk6 && which admincli && admincli tg && admincli ts -f /tmp/tmp.fhblBC6Az0 -ch 614902946"
						);
		
		System.out.println(result.stdouterr());
		System.out.println(result.exitCode());
		assertTrue(0 == result.exitCode());
	}
}
