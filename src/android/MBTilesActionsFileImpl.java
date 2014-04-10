package com.makina.offline.mbtiles;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import android.util.Base64;
import android.util.Log;

/**
 * {@link IMBTilesActions} filesystem implementation.
 * 
 * @author <a href="mailto:sebastien.grimault@makina-corpus.com">S. Grimault</a>
 */
public class MBTilesActionsFileImpl implements IMBTilesActions
{
	// declaration of path of file
	private File mapDirectory = null;
	
	@Override
	public void open(String path)
	{
		close();

		Log.d(getClass().getName(), "try to open map directory '" + path + "'");
		
		this.mapDirectory = new File(path);
	}

	@Override
	public boolean isOpen()
	{
		return this.mapDirectory != null;
	}
	
	@Override
	public void close()
	{
		if (isOpen())
		{
			Log.d(getClass().getName(), "close '" + mapDirectory.getAbsolutePath() + "'");
		}
		this.mapDirectory = null;
	}

	@Override
	public JSONObject getMetadata()
	{
		File fileContentMedata = new File(this.mapDirectory.getAbsolutePath() + File.separator + "metadata.json");
		
		JSONObject metadata = new JSONObject();

		if (fileContentMedata.exists()) {
		
			try
			{
				metadata = new JSONObject(FileUtils.readFileAsString(fileContentMedata));
			}
			catch (JSONException je)
			{
				Log.e(getClass().getName(), je.getMessage(), je);
			}
			catch (IOException ioe)
			{
				Log.e(getClass().getName(), ioe.getMessage(), ioe);
			}
		}
		return metadata;
	}

	@Override
	public JSONObject getMinZoom()
	{
		JSONObject minZoom = new JSONObject();
		
		try
		{
			List<Integer> zoomLevels = getZoomLevels();
			
			if (!zoomLevels.isEmpty())
			{
				minZoom.put(KEY_MIN_ZOOM, zoomLevels.get(0));
			}
		}
		catch (JSONException je)
		{
			Log.e(getClass().getName(), je.getMessage(), je);
		}
		
		return minZoom;
	}

	@Override
	public JSONObject getMaxZoom()
	{
		JSONObject maxZoom = new JSONObject();
		
		try
		{
			List<Integer> zoomLevels = getZoomLevels();
			
			if (!zoomLevels.isEmpty())
			{
				maxZoom.put(KEY_MAX_ZOOM, zoomLevels.get(zoomLevels.size() - 1));
			}
		}
		catch (JSONException je)
		{
			Log.e(getClass().getName(), je.getMessage(), je);
		}
		
		return maxZoom;
	}

	@Override
	public JSONObject getTile(int zoomLevel, int column, int row)
	{
		JSONObject tileData = new JSONObject();
		JSONObject metadata = getMetadata();
		ByteArrayOutputStream baos = null;
		FileInputStream in = null;
		BufferedOutputStream out = null;
		
		try
		{
			String name = (String) metadata.get(KEY_NAME);
			String version = (String) metadata.get(KEY_VERSION);
			String format = (String) metadata.get(KEY_FORMAT);
			
			// try to load the last zoom level if zoomLevel is too high
			int maxZoom = getMaxZoom().getInt(KEY_MAX_ZOOM);
			int currentZoomLevel = zoomLevel;
			
			if (zoomLevel > maxZoom)
			{
				currentZoomLevel = maxZoom;
			}
			
			File tileFile = new File(this.mapDirectory.getAbsolutePath() + File.separator + version + File.separator + name + File.separator + String.valueOf(currentZoomLevel) + File.separator + String.valueOf(column) + File.separator + String.valueOf(row) + "." + format);
			
			// test if file exist
			if (tileFile.exists())
			{
				baos = new ByteArrayOutputStream();
				in = new FileInputStream(tileFile);
				out = new BufferedOutputStream(baos);
				FileUtils.copyInputStream(in,out);
				
				tileData.put(KEY_TILE_DATA, Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT));

			}
		}
		catch (JSONException je)
		{
			Log.e(getClass().getName(), je.getMessage(), je);
		}
		catch (FileNotFoundException fnfe)
		{
			Log.e(getClass().getName(), fnfe.getMessage(), fnfe);
		}
		catch (IOException ioe)
		{
			Log.e(getClass().getName(), ioe.getMessage(), ioe);
		} finally {
			
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
			    		// nothing to do here except log the exception
				}
			}
			
			if (out != null) {
				try {
					out.close();
				} catch (IOException e) {
			    		// nothing to do here except log the exception
				}
			}
			if (baos != null) {
				try {
					baos.close();
				} catch (IOException e) {
			    		// nothing to do here except log the exception
				}
			}
		}
		
		
		return tileData;
	}

	/**
	 * Tries to retrieve all available zoom levels for this MBTiles resource.
	 * @return a {@link List} of zoom levels as {@link Integer}
	 */
	private List<Integer> getZoomLevels()
	{
		JSONObject metadata = getMetadata();
		List<Integer> zoomLevels = new ArrayList<Integer>();
		
		try
		{
			String name = (String) metadata.get(KEY_NAME);
			String version = (String) metadata.get(KEY_VERSION);
			
			File contents = new File(this.mapDirectory.getAbsolutePath() + File.separator + version + File.separator + name);
			
			if (contents.exists()) {
				for (File zoomLevel : contents.listFiles())
				{
					zoomLevels.add(Integer.valueOf(zoomLevel.getName()));
				}
				Arrays.sort(zoomLevels.toArray(new Integer[]{}));
			}
		}
		catch (JSONException je)
		{
			Log.e(getClass().getName(), je.getMessage(), je);
		}
		
		return zoomLevels;
	}
	
	@Override
	public JSONObject executeStatment(String query, String... params) {
		// not implemented
		JSONObject result = new JSONObject();
		return result;
	}

	/**
	 * return the directory of working
	 * @param path : the pah of working
	 * @return <code>JSONObject</code>
	 */
	@Override
	public JSONObject getDirectoryWorking(String path) {
		JSONObject directoryWorking = new JSONObject();
		try {
			directoryWorking.put(KEY_DIRECTORY_WORKING, path);
		} catch (JSONException e) {
			Log.w(getClass().getName(), e.getMessage());
		}
		return directoryWorking;
	}
}
