package com.supai.app.services.gdrive.api;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class TestAPI {
	private final ListSubFilderGdrive listSubFilderGdrive;
	private final CreateFolderGDrive createFolderGDrive;
	private final ListSubFilesGDrive listSubFilesGDrive;
	private final GetNodeFormDrive getNodeFormDrive;
	private final ObjectMapper objectMapper;

	@Value("${gdrive.folder.root}")
	private String gdriveRootFolder;

	public void test() {
		//getNodeInfo();
		//creatSubFolders();
		//listSubFolders();
		printFiles();
	}

	public void getNodeInfo() {
		File fileNode = getNodeFormDrive.getNodeInfo(gdriveRootFolder);
		if (fileNode != null) {
			pringJsonString(fileNode.toString());
		}

	}

	public void listSubFolders() {
		FileList subFolders = listSubFilderGdrive.getSubFolders(gdriveRootFolder);
		if (subFolders != null) {
			pringJsonString(subFolders.toString());
		}
	}

	public void creatSubFolders() {
		File subFoldersId = createFolderGDrive.create("TestFolder", gdriveRootFolder);
		if (subFoldersId != null) {
			pringJsonString(subFoldersId.toString());
		}
	}

	public void printFiles() {
		List<File> listFilesInFolder = listSubFilesGDrive.listFiles(gdriveRootFolder);
		if (listFilesInFolder != null) {
			pringJsonString(listFilesInFolder.toString());
		}
	}

	private String pringJsonString(String jsonStr) {
		try {
			String prityJsonStr = objectMapper.readTree(jsonStr).toPrettyString();
			log.info("{}", prityJsonStr);
		} catch (Exception e) {
			log.error("{}", e.toString());
		}
		return null;
	}
}
