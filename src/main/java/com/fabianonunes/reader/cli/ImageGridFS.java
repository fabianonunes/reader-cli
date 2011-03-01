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

		Mongo m = new Mongo("10.0.223.163");

		DB db = m.getDB("sesdi2");
		
		db.authenticate("fabiano_sesdi2", "timestamp-2010".toCharArray());

		GridFS gfs = new GridFS(db, "images");

		Pattern john = Pattern.compile("^inicial", Pattern.CASE_INSENSITIVE);
		
		BasicDBObject query = new BasicDBObject("filename", john);

		gfs.remove(query);

		File dir = new File("/home/fabiano/workdir/png");

		File[] files = dir.listFiles(pngFilter);

		for (File file : files) {

			GridFSInputFile gfsFile = gfs.createFile(file);
			
			gfsFile.setContentType("image/png");

			gfsFile.setFilename("inicial/" + file.getName());

			gfsFile.save();

		}

	}

}
