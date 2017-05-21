package com.github.dakusui.cmd;

import com.github.dakusui.cmd.scenario.PipelineTest;
import com.github.dakusui.cmd.scenario.ScenarioTest;
import com.github.dakusui.cmd.ut.CmdStateTest;
import com.github.dakusui.cmd.ut.CmdTest;
import com.github.dakusui.cmd.ut.CommandRunnerTest;
import com.github.dakusui.cmd.ut.SelectorTest;
import com.github.dakusui.cmd.ut.io.LineReaderTest;
import com.github.dakusui.cmd.ut.io.RingBufferedLineWriterTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
    LineReaderTest.class,
    RingBufferedLineWriterTest.class,
    CommandRunnerTest.class,
    CmdTest.class,
    CmdStateTest.class,
    SelectorTest.class,
    ScenarioTest.class,
    PipelineTest.class
})
public class All {
}
