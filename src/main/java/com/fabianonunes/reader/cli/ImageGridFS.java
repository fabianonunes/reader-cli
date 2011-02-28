package com.fabianonunes.reader.cli;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.regex.Pattern;

import org.apache.commons.io.filefilter.FileFilterUtils;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.Mongo;
import com.mongodb.MongoException;
import com.mongodb.gridfs.GridFS;
import com.mongodb.gridfs.GridFSInputFile;

public class ImageGridFS {

	public static FileFilter pngFilter = FileFilterUtils
			.suffixFileFilter(".png");

	public static void main(String[] args) throws MongoException, IOException {

		Mongo m = new Mongo();

		DB db = m.getDB("sesdi2");

		GridFS gfs = new GridFS(db, "images");

		Pattern john = Pattern.compile("^inicial", Pattern.CASE_INSENSITIVE);
		BasicDBObject query = new BasicDBObject("filename", john);

		gfs.remove(query);

		File dir = new File(
				"/media/TST02/Processos/inicial/images/png");

		File[] files = dir.listFiles(pngFilter);

		for (File file : files) {

			GridFSInputFile gfsFile = gfs.createFile(file);

			gfsFile.setContentType("image/png");

			gfsFile.setFilename("inicial/"
					+ file.getName().replace(".png.gz", ""));

			gfsFile.save();

		}

		dir = new File("/media/TST02/Processos/inicial/images/png/t");

		files = dir.listFiles(pngFilter);

		for (File file : files) {

			GridFSInputFile gfsFile = gfs.createFile(file);

			gfsFile.setContentType("image/png");

			gfsFile.setFilename("inicial/t/"
					+ file.getName().replace(".png.gz", ""));

			gfsFile.save();

		}

	}

}
