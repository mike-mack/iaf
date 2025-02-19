package nl.nn.adapterframework.filesystem;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.PipeLine.ExitState;
import nl.nn.adapterframework.core.PipeLineResult;
import nl.nn.adapterframework.core.ProcessState;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.DateUtils;

public abstract class FileSystemListenerTest<F, FS extends IBasicFileSystem<F>> extends HelperedFileSystemTestBase {


	protected String fileAndFolderPrefix="";
	protected boolean testFullErrorMessages=true;

	protected FileSystemListener<F,FS> fileSystemListener;
	protected Map<String,Object> threadContext;


	public abstract FileSystemListener<F,FS> createFileSystemListener();

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		fileSystemListener = createFileSystemListener();
		autowireByName(fileSystemListener);
		threadContext=new HashMap<String,Object>();
	}

	@Override
	@After
	public void tearDown() throws Exception {
		if (fileSystemListener!=null) {
			fileSystemListener.close();
		};
		super.tearDown();
	}


	@Test
	public void fileListenerTestConfigure() throws Exception {
		fileSystemListener.configure();
	}

	@Test
	public void fileListenerTestOpen() throws Exception {
		fileSystemListener.configure();
		fileSystemListener.open();
	}


	@Test
	public void fileListenerTestInvalidInputFolder() throws Exception {
		String folder=fileAndFolderPrefix+"xxx";
		fileSystemListener.setInputFolder(folder);
		if (testFullErrorMessages) {
			exception.expectMessage("The value for inputFolder ["+folder+"], canonical name [");
			exception.expectMessage("It is not a folder.");
		} else {
			exception.expectMessage("["+folder+"] is invalid.");
		}
		fileSystemListener.configure();
		fileSystemListener.open();
	}

	@Test
	public void fileListenerTestInvalidInProcessFolder() throws Exception {
		String folder=fileAndFolderPrefix+"xxx";
		fileSystemListener.setInProcessFolder(folder);
		if (testFullErrorMessages) {
			exception.expectMessage("The value for inProcessFolder ["+folder+"], canonical name [");
			exception.expectMessage("It is not a folder.");
		} else {
			exception.expectMessage("["+folder+"] is invalid.");
		}
		fileSystemListener.configure();
		fileSystemListener.open();
	}

	@Test
	public void fileListenerTestInvalidProcessedFolder() throws Exception {
		String folder=fileAndFolderPrefix+"xxx";
		fileSystemListener.setProcessedFolder(folder);
		if (testFullErrorMessages) {
			exception.expectMessage("The value for processedFolder ["+folder+"], canonical name [");
			exception.expectMessage("It is not a folder.");
		} else {
			exception.expectMessage("["+folder+"] is invalid.");
		}
		fileSystemListener.configure();
		fileSystemListener.open();
	}

	@Test
	public void fileListenerTestCreateInputFolder() throws Exception {
		fileSystemListener.setInputFolder(fileAndFolderPrefix+"xxx1");
		fileSystemListener.setCreateFolders(true);
		fileSystemListener.configure();
		fileSystemListener.open();
	}

	@Test
	public void fileListenerTestCreateInProcessFolder() throws Exception {
		fileSystemListener.setInProcessFolder(fileAndFolderPrefix+"xxx2");
		fileSystemListener.setCreateFolders(true);
		fileSystemListener.configure();
		fileSystemListener.open();
	}

	@Test
	public void fileListenerTestCreateProcessedFolder() throws Exception {
		fileSystemListener.setProcessedFolder(fileAndFolderPrefix+"xxx3");
		fileSystemListener.setCreateFolders(true);
		fileSystemListener.configure();
		fileSystemListener.open();
	}

	@Test
	public void fileListenerTestCreateLogFolder() throws Exception {
		fileSystemListener.setLogFolder(fileAndFolderPrefix+"xxx4");
		fileSystemListener.setCreateFolders(true);
		fileSystemListener.configure();
		fileSystemListener.open();
	}

	/*
	 * vary this on:
	 *   inputFolder
	 *   inProcessFolder
	 */
	public void fileListenerTestGetRawMessage(String inputFolder, String inProcessFolder) throws Exception {
		String filename="rawMessageFile";
		String contents="Test Message Contents";

		fileSystemListener.setMinStableTime(0);
		if (inputFolder!=null) {
			fileSystemListener.setInputFolder(inputFolder);
		}
		if (inProcessFolder!=null) {
			fileSystemListener.setInProcessFolder(fileAndFolderPrefix+inProcessFolder);
			_createFolder(inProcessFolder);
			waitForActionToFinish();
		}
		fileSystemListener.configure();
		fileSystemListener.open();

		F rawMessage=fileSystemListener.getRawMessage(threadContext);
		assertNull("raw message must be null when not available",rawMessage);

		createFile(null, filename, contents);

		rawMessage=fileSystemListener.getRawMessage(threadContext);
		assertNotNull("raw message must be not null when a file is available",rawMessage);


		F secondMessage=fileSystemListener.getRawMessage(threadContext);
		if (inProcessFolder!=null) {
			boolean movedFirst = fileSystemListener.changeProcessState(rawMessage, ProcessState.INPROCESS, null)!=null;
			boolean movedSecond = fileSystemListener.changeProcessState(secondMessage, ProcessState.INPROCESS, null)!=null;
			assertFalse("raw message not have been moved by both threads",movedFirst && movedSecond);
		} else {
			assertNotNull("raw message must still be available when no inProcessFolder is configured",secondMessage);
		}

	}

	@Test
	public void fileListenerTestGetRawMessage() throws Exception {
		fileListenerTestGetRawMessage(null,null);
	}

	@Test
	public void fileListenerTestGetRawMessageWithInProcess() throws Exception {
		String folderName = "inProcessFolder";
		fileListenerTestGetRawMessage(null,folderName);
	}

	@Test
	public void fileListenerTestMoveToInProcessMustFailIfFileAlreadyExistsInInProcessFolder() throws Exception {
		String inProcessFolder = "inProcessFolder";
		String filename="rawMessageFile";
		_createFolder(inProcessFolder);
		waitForActionToFinish();
		createFile(null, filename, "fakeNewFileContents");
		createFile(inProcessFolder, filename, "fakeExistingFileContents");
		waitForActionToFinish();

		fileSystemListener.setMinStableTime(0);
		fileSystemListener.setInProcessFolder(fileAndFolderPrefix+inProcessFolder);
		fileSystemListener.configure();
		fileSystemListener.open();

		assertThrows(ListenerException.class, ()-> {
			F rawMessage=fileSystemListener.getRawMessage(threadContext);
			fileSystemListener.changeProcessState(rawMessage, ProcessState.INPROCESS, "test");
			assertNull(rawMessage);
		});
	}

	@Test
	public void fileListenerTestGetRawMessageWithInProcessTimeSensitive() throws Exception {
		String folderName = "inProcessFolder";

		String filename="rawMessageFile";
		String contents="Test Message Contents";

		fileSystemListener.setFileTimeSensitive(true);
		fileSystemListener.setMinStableTime(0);
		fileSystemListener.setInProcessFolder(fileAndFolderPrefix+folderName);
		_createFolder(folderName);

		waitForActionToFinish();

		fileSystemListener.configure();
		fileSystemListener.open();

		F rawMessage=fileSystemListener.getRawMessage(threadContext);
		assertNull("raw message must be null when not available",rawMessage);

		createFile(null, filename, contents);

		rawMessage=fileSystemListener.getRawMessage(threadContext);
		assertNotNull("raw message must be not null when a file is available",rawMessage);

		F movedFile = fileSystemListener.changeProcessState(rawMessage, ProcessState.INPROCESS, null);
		assertTrue(fileSystemListener.getFileSystem().getName(movedFile).startsWith(filename+"-"));
	}

	@Test
	public void changeProcessStateForTwoFilesWithTheSameName() throws Exception {
		String folderName = "inProcessFolder";

		String filename="rawMessageFile";
		String contents="Test Message Contents";

		fileSystemListener.setFileTimeSensitive(true);
		fileSystemListener.setMinStableTime(0);
		fileSystemListener.setInProcessFolder(fileAndFolderPrefix+folderName);
		_createFolder(folderName);

		waitForActionToFinish();

		fileSystemListener.configure();
		fileSystemListener.open();

		F rawMessage=fileSystemListener.getRawMessage(threadContext);
		assertNull("raw message must be null when not available",rawMessage);

		createFile(null, filename, contents);

		rawMessage=fileSystemListener.getRawMessage(threadContext);
		assertNotNull("raw message must be not null when a file is available",rawMessage);

		F movedFile = fileSystemListener.changeProcessState(rawMessage, ProcessState.INPROCESS, null);
		assertTrue(fileSystemListener.getFileSystem().getName(movedFile).startsWith(filename+"-"));

		createFile(null, filename, contents);
		F rawMessage2 = fileSystemListener.getRawMessage(threadContext);
		F movedFile2 = fileSystemListener.changeProcessState(rawMessage2, ProcessState.INPROCESS, null);
		assertTrue(fileSystemListener.getFileSystem().getName(movedFile2).startsWith(filename+"-"));

		assertNotEquals(fileSystemListener.getFileSystem().getName(movedFile), fileSystemListener.getFileSystem().getName(movedFile2));
	}

	@Ignore("This fails in some operating systems since copying file may change the modification date") // TODO: mock getModificationTime
	@Test
	public void changeProcessStateForTwoFilesWithTheSameNameAndTimestamp() throws Exception {
		String folderName = "inProcessFolder";
		String copiedFileFolderName="copiedFile";

		String filename="rawMessageFile";
		String contents="Test Message Contents";

		fileSystemListener.setFileTimeSensitive(true);
		fileSystemListener.setMinStableTime(0);
		fileSystemListener.setInProcessFolder(fileAndFolderPrefix+folderName);
		_createFolder(folderName);

		waitForActionToFinish();

		fileSystemListener.configure();
		fileSystemListener.open();

		F rawMessage=fileSystemListener.getRawMessage(threadContext);
		assertNull("raw message must be null when not available",rawMessage);

		createFile(null, filename, contents);
		F f = fileSystemListener.getFileSystem().toFile(fileAndFolderPrefix+filename);
		F copiedFile = fileSystemListener.getFileSystem().copyFile(f, fileAndFolderPrefix+copiedFileFolderName, true, true);

		rawMessage=fileSystemListener.getRawMessage(threadContext);
		assertNotNull("raw message must be not null when a file is available",rawMessage);

		F movedFile = fileSystemListener.changeProcessState(rawMessage, ProcessState.INPROCESS, null);
		assertTrue(fileSystemListener.getFileSystem().getName(movedFile).startsWith(filename+"-"));

		F movedCopiedFile = fileSystemListener.getFileSystem().moveFile(copiedFile, fileAndFolderPrefix, true, true);

		Date modificationDateFile = fileSystemListener.getFileSystem().getModificationTime(movedCopiedFile);
		Date modificationDateSecondFile = fileSystemListener.getFileSystem().getModificationTime(f);
		assertEquals(modificationDateFile, modificationDateSecondFile);

		F movedFile2 = fileSystemListener.changeProcessState(movedCopiedFile, ProcessState.INPROCESS, null);

		String nameOfFirstFile = fileSystemListener.getFileSystem().getName(movedFile);
		String nameOfSecondFile = fileSystemListener.getFileSystem().getName(movedFile2);

		assertEquals(nameOfFirstFile, nameOfSecondFile.substring(0, nameOfSecondFile.lastIndexOf("-")));
	}

	@Ignore("This fails in some operating systems since copying file may change the modification date")
	@Test
	public void changeProcessStateFor6FilesWithTheSameNameAndTimestamp() throws Exception {
		String folderName = "inProcessFolder";
		String copiedFileFolderName="copiedFile";

		String filename="rawMessageFile";
		String contents="Test Message Contents";

		fileSystemListener.setFileTimeSensitive(true);
		fileSystemListener.setMinStableTime(0);
		fileSystemListener.setInProcessFolder(fileAndFolderPrefix+folderName);
		_createFolder(folderName);

		waitForActionToFinish();

		fileSystemListener.configure();
		fileSystemListener.open();

		F rawMessage=fileSystemListener.getRawMessage(threadContext);
		assertNull("raw message must be null when not available",rawMessage);

		createFile(null, filename, contents);
		F f = fileSystemListener.getFileSystem().toFile(fileAndFolderPrefix+filename);
		Date modificationDateFirstFile = fileSystemListener.getFileSystem().getModificationTime(f);
		// copy file
		for(int i=1;i<=6;i++) {
			fileSystemListener.getFileSystem().copyFile(f, fileAndFolderPrefix+copiedFileFolderName+i, true, false);
		}

		rawMessage=fileSystemListener.getRawMessage(threadContext);
		assertNotNull("raw message must be not null when a file is available",rawMessage);

		F movedFile = fileSystemListener.changeProcessState(rawMessage, ProcessState.INPROCESS, null);
		assertTrue(fileSystemListener.getFileSystem().getName(movedFile).startsWith(filename+"-"));

		String nameOfFirstFile = fileSystemListener.getFileSystem().getName(movedFile);


		for(int i=1;i<=6;i++) {
			F movedCopiedFile = fileSystemListener.getFileSystem().moveFile(fileSystemListener.getFileSystem().toFile(fileAndFolderPrefix+copiedFileFolderName+i, filename), fileAndFolderPrefix, true, true);

			Date modificationDate = fileSystemListener.getFileSystem().getModificationTime(movedCopiedFile);
			assertEquals(modificationDateFirstFile.getTime(), modificationDate.getTime());

			F movedFile2 = fileSystemListener.changeProcessState(movedCopiedFile, ProcessState.INPROCESS, null);

			String nameOfSecondFile = fileSystemListener.getFileSystem().getName(movedFile2);

			if(i==6) {
				assertEquals(filename, nameOfSecondFile);
			} else {
				assertEquals(nameOfFirstFile+"-"+i, nameOfSecondFile);
			}
		}
	}

	@Test
	public void fileListenerTestGetStringFromRawMessageFilename() throws Exception {
		String filename="rawMessageFile";
		String contents="Test Message Contents";

		fileSystemListener.setMinStableTime(0);
		fileSystemListener.configure();
		fileSystemListener.open();

		createFile(null, filename, contents);

		F rawMessage=fileSystemListener.getRawMessage(threadContext);
		assertNotNull(rawMessage);

		Message message=fileSystemListener.extractMessage(rawMessage, threadContext);
		assertThat(message.asString(),containsString(filename));
	}

	@Test
	public void fileListenerTestGetStringFromRawMessageContents() throws Exception {
		String filename="rawMessageFile";
		String contents="Test Message Contents";

		fileSystemListener.setMinStableTime(0);
		fileSystemListener.setMessageType("contents");
		fileSystemListener.configure();
		fileSystemListener.open();

		createFile(null, filename, contents);

		F rawMessage=fileSystemListener.getRawMessage(threadContext);
		assertNotNull(rawMessage);

		Message message=fileSystemListener.extractMessage(rawMessage, threadContext);
		assertEquals(contents,message.asString());
	}

//	@Test
//	public void fileListenerTestGetStringFromRawMessageOtherAttribute() throws Exception {
//		throw new NotImplementedException();
//	}


	/*
	 * Test for proper id
	 * Test for additionalProperties in session variables
	 */
	@Test
	public void fileListenerTestGetIdFromRawMessage() throws Exception {
		String filename="rawMessageFile";
		String contents="Test Message Contents";

		fileSystemListener.setMinStableTime(0);
		fileSystemListener.configure();
		fileSystemListener.open();

		createFile(null, filename, contents);

		F rawMessage=fileSystemListener.getRawMessage(threadContext);
		assertNotNull(rawMessage);

		String id=fileSystemListener.getIdFromRawMessage(rawMessage, threadContext);
		assertThat(id,endsWith(filename));

		String filenameAttribute = (String)threadContext.get("filename");
		assertThat(filenameAttribute, containsString(filename));

	}

	@Test
	public void fileListenerTestGetIdFromRawMessageMessageTypeName() throws Exception {
		String filename="rawMessageFile";
		String contents="Test Message Contents";

		fileSystemListener.setMinStableTime(0);
		fileSystemListener.setMessageType("name");
		fileSystemListener.configure();
		fileSystemListener.open();

		createFile(null, filename, contents);

		F rawMessage=fileSystemListener.getRawMessage(threadContext);
		assertNotNull(rawMessage);

		String id=fileSystemListener.getIdFromRawMessage(rawMessage, threadContext);
		assertThat(id,endsWith(filename));

		String filepathAttribute = (String)threadContext.get("filepath");
		assertThat(filepathAttribute, containsString(filename));
	}

	@Test
	public void fileListenerTestGetIdFromRawMessageWithMetadata() throws Exception {
		String filename="rawMessageFile";
		String contents="Test Message Contents";

		fileSystemListener.setMinStableTime(0);
		fileSystemListener.setStoreMetadataInSessionKey("metadata");
		fileSystemListener.configure();
		fileSystemListener.open();

		createFile(null, filename, contents);

		F rawMessage=fileSystemListener.getRawMessage(threadContext);
		assertNotNull(rawMessage);

		String id=fileSystemListener.getIdFromRawMessage(rawMessage, threadContext);
		assertThat(id,endsWith(filename));

		String metadataAttribute = (String)threadContext.get("metadata");
		System.out.println(metadataAttribute);
		assertThat(metadataAttribute, startsWith("<metadata"));
	}

	/*
	 * Test for proper id
	 * Test for additionalProperties in session variables
	 */
	@Test
	public void fileListenerTestGetIdFromRawMessageFileTimeSensitive() throws Exception {
		String filename="rawMessageFile";
		String contents="Test Message Contents";

		fileSystemListener.setMinStableTime(0);
		fileSystemListener.setFileTimeSensitive(true);
		fileSystemListener.configure();
		fileSystemListener.open();

		createFile(null, filename, contents);

		F rawMessage=fileSystemListener.getRawMessage(threadContext);
		assertNotNull(rawMessage);

		String id=fileSystemListener.getIdFromRawMessage(rawMessage, threadContext);
		assertThat(id, containsString(filename));
		String currentDateFormatted=DateUtils.format(new Date());
		String timestamp=id.substring(id.length()-currentDateFormatted.length());
		long currentDate=DateUtils.parseAnyDate(currentDateFormatted).getTime();
		long timestampDate=DateUtils.parseAnyDate(timestamp).getTime();
		assertTrue(Math.abs(timestampDate-currentDate)<7300000); // less then two hours in milliseconds.
	}

	@Test
	public void fileListenerTestAfterMessageProcessedDeleteAndCopy() throws Exception {
		String filename = "AfterMessageProcessedDelete" + FILE1;
		String logFolder = "logFolder";
		String contents = "contents of file";

		fileSystemListener.setMinStableTime(0);
		fileSystemListener.setDelete(true);
		fileSystemListener.setLogFolder(fileAndFolderPrefix+logFolder);
		fileSystemListener.setCreateFolders(true);
		fileSystemListener.configure();
		fileSystemListener.open();

		createFile(null, filename, contents);
		waitForActionToFinish();
		// test
		existsCheck(filename);

		F rawMessage=fileSystemListener.getRawMessage(threadContext);
		assertNotNull(rawMessage);
		PipeLineResult processResult = new PipeLineResult();
		processResult.setState(ExitState.SUCCESS);
		fileSystemListener.afterMessageProcessed(processResult, rawMessage, null);
		waitForActionToFinish();
		// test
		assertFalse("Expected file [" + filename + "] not to be present", _fileExists(filename));
		assertFileExistsWithContents(logFolder, filename, contents);
	}


	@Test
	public void fileListenerTestAfterMessageProcessedMoveFile() throws Exception {
		String fileName = "fileTobeMoved.txt";
		String processedFolder = "destinationFolder";

		createFile(null,fileName, "");
		waitForActionToFinish();

		assertTrue(_fileExists(fileName));

		_createFolder(processedFolder);
		waitForActionToFinish();

		fileSystemListener.setMinStableTime(0);
		fileSystemListener.setProcessedFolder(fileAndFolderPrefix+processedFolder);
		fileSystemListener.configure();
		fileSystemListener.open();


		assertTrue(_fileExists(fileName));
		assertTrue(_folderExists(processedFolder));

		F rawMessage=fileSystemListener.getRawMessage(threadContext);
		assertNotNull(rawMessage);
		PipeLineResult processResult = new PipeLineResult();
		processResult.setState(ExitState.SUCCESS);
		fileSystemListener.changeProcessState(rawMessage, ProcessState.DONE, "test");
		fileSystemListener.afterMessageProcessed(processResult, rawMessage, null);
		waitForActionToFinish();


		assertTrue("Destination folder must exist",_folderExists(processedFolder));
		assertTrue("Destination must exist",_fileExists(processedFolder, fileName));
		assertFalse("Origin must have disappeared",_fileExists(fileName));
	}

	@Test
	public void fileListenerTestAfterMessageProcessedMoveFileOverwrite() throws Exception {
		String fileName = "fileTobeMoved.txt";
		String processedFolder = "destinationFolder";

		createFile(null,fileName, "");
		waitForActionToFinish();

		assertTrue(_fileExists(fileName));

		_createFolder(processedFolder);
		createFile(processedFolder,fileName, "");
		waitForActionToFinish();

		fileSystemListener.setMinStableTime(0);
		fileSystemListener.setProcessedFolder(fileAndFolderPrefix+processedFolder);
		fileSystemListener.setOverwrite(true);
		fileSystemListener.configure();
		fileSystemListener.open();


		assertTrue(_fileExists(fileName));
		assertTrue(_folderExists(processedFolder));

		F rawMessage=fileSystemListener.getRawMessage(threadContext);
		assertNotNull(rawMessage);
		PipeLineResult processResult = new PipeLineResult();
		processResult.setState(ExitState.SUCCESS);
		fileSystemListener.changeProcessState(rawMessage, ProcessState.DONE, "test");
		fileSystemListener.afterMessageProcessed(processResult, rawMessage, null);
		waitForActionToFinish();


		assertTrue("Destination folder must exist",_folderExists(processedFolder));
		assertTrue("Destination must exist",_fileExists(processedFolder, fileName));
		assertFalse("Origin must have disappeared",_fileExists(fileName));
	}

	@Test
	public void fileListenerTestAfterMessageProcessedErrorDelete() throws Exception {
		String filename = "AfterMessageProcessedDelete" + FILE1;

		fileSystemListener.setMinStableTime(0);
		fileSystemListener.setDelete(true);
		fileSystemListener.configure();
		fileSystemListener.open();

		createFile(null, filename, "maakt niet uit");
		waitForActionToFinish();
		// test
		existsCheck(filename);

		F rawMessage=fileSystemListener.getRawMessage(threadContext);
		assertNotNull(rawMessage);
		PipeLineResult processResult = new PipeLineResult();
		processResult.setState(ExitState.ERROR);
		fileSystemListener.afterMessageProcessed(processResult, rawMessage, null);
		waitForActionToFinish();
		// test
		assertFalse("Expected file [" + filename + "] not to be present", _fileExists(filename));
	}


	@Test
	public void fileListenerTestAfterMessageProcessedErrorMoveFileToErrorFolder() throws Exception {
		String fileName = "fileTobeMoved.txt";
		String processedFolder = "destinationFolder";
		String errorFolder = "errorFolder";

		createFile(null,fileName, "");
		waitForActionToFinish();

		assertTrue(_fileExists(fileName));

		_createFolder(processedFolder);
		_createFolder(errorFolder);
		waitForActionToFinish();

		fileSystemListener.setMinStableTime(0);
		fileSystemListener.setProcessedFolder(fileAndFolderPrefix+processedFolder);
		fileSystemListener.setErrorFolder(fileAndFolderPrefix+errorFolder);
		fileSystemListener.configure();
		fileSystemListener.open();


		assertTrue(_fileExists(fileName));
		assertTrue(_folderExists(processedFolder));

		F rawMessage=fileSystemListener.getRawMessage(threadContext);
		assertNotNull(rawMessage);
		PipeLineResult processResult = new PipeLineResult();
		processResult.setState(ExitState.ERROR);
		fileSystemListener.changeProcessState(rawMessage, ProcessState.ERROR, "test");  // this action was formerly executed by afterMessageProcessed(), but has been moved to Receiver.moveInProcessToError()
		fileSystemListener.afterMessageProcessed(processResult, rawMessage, null);
		waitForActionToFinish();


		assertTrue("Error folder must exist",_folderExists(processedFolder));
		assertTrue("Destination must exist in error folder",_fileExists(errorFolder, fileName));
		assertTrue("Destination must not exist in processed folder",!_fileExists(processedFolder, fileName));
		assertFalse("Origin must have disappeared",_fileExists(fileName));
	}
	@Test
	public void fileListenerTestAfterMessageProcessedErrorMoveFileToProcessedFolder() throws Exception {
		String fileName = "fileTobeMoved.txt";
		String processedFolder = "destinationFolder";

		createFile(null,fileName, "");
		waitForActionToFinish();

		assertTrue(_fileExists(fileName));

		_createFolder(processedFolder);
		waitForActionToFinish();

		fileSystemListener.setMinStableTime(0);
		fileSystemListener.setProcessedFolder(fileAndFolderPrefix+processedFolder);
		fileSystemListener.configure();
		fileSystemListener.open();


		assertTrue(_fileExists(fileName));
		assertTrue(_folderExists(processedFolder));

		F rawMessage=fileSystemListener.getRawMessage(threadContext);
		assertNotNull(rawMessage);
		PipeLineResult processResult = new PipeLineResult();
		processResult.setState(ExitState.ERROR);
		fileSystemListener.changeProcessState(rawMessage, ProcessState.DONE, "test"); // this action was formerly executed by afterMessageProcessed(), but has been moved to Receiver.moveInProcessToError()
		fileSystemListener.afterMessageProcessed(processResult, rawMessage, null);
		waitForActionToFinish();


		assertTrue("Error folder must exist",_folderExists(processedFolder));
		assertTrue("Destination must exist in processed folder",_fileExists(processedFolder, fileName));
		assertFalse("Origin must have disappeared",_fileExists(fileName));
	}


}
