package com.github.dakusui.cmd;

import com.github.dakusui.cmd.ut.*;
import com.github.dakusui.cmd.ut.io.IoUtilsTest;
import com.github.dakusui.cmd.ut.io.LineReaderTest;
import com.github.dakusui.cmd.ut.io.RingBufferedLineWriterTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
    CmdStateTest.class,
    CmdTest.class,
    CommandUtilsTest.class,
    IoUtilsTest.class,
    LineReaderTest.class,
    PipelinedCmdTest.class,
    SelectorTest.class,
    StreamableProcessTest.class,
    StreamableQueueTest.class,
    RingBufferedLineWriterTest.class,
})
public class All {
}
