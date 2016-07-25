import java.net.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.file.*;
import java.util.*;
import java.lang.*;

class FileSplit {
	public static void splitFile(File f) throws IOException {
		int partCounter = 1;//chunk number

		int sizeOfFiles = 100 * 1024;// 100kb
		byte[] buffer = new byte[sizeOfFiles];

		try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(f))) {
			String name = f.getName();

			int tmp = 0;
			while ((tmp = bis.read(buffer)) > 0) {
				
				
				File newFile = new File(Server.chunk_dir,String.format("%03d", partCounter++)+".txt" );

				try (FileOutputStream out = new FileOutputStream(newFile)) {
					out.write(buffer, 0, tmp);// tmp is chunk size

				}
			}
		}
	}
	
	public static void mergeFiles(List<String> files, String into) throws IOException {
	    try (BufferedOutputStream mergingStream = new BufferedOutputStream(new FileOutputStream(into))) {
	      for (String s : files) {
	      	File f = new File(s);
	        Files.copy(f.toPath(), mergingStream);
	      }
	    }
	}

}