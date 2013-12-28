/**
 * Copyright (C) 2008 Happy Fish / YuQing
 *
 * FastDFS Java Client may be copied only under the terms of the GNU Lesser
 * General Public License (LGPL).
 * Please visit the FastDFS Home Page http://www.csource.org/ for more detail.
 **/

package org.danielli.xultimate.context.dfs;

import java.io.File;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Resource;

import org.csource.fastdfs.ClientGlobal;
import org.csource.fastdfs.ProtoCommon;
import org.csource.fastdfs.ServerInfo;
import org.csource.fastdfs.StorageClient;
import org.csource.fastdfs.StorageClient1;
import org.csource.fastdfs.StorageServer;
import org.csource.fastdfs.TrackerClient;
import org.csource.fastdfs.TrackerServer;
import org.danielli.xultimate.context.dfs.fastdfs.StorageClientCallback;
import org.danielli.xultimate.context.dfs.fastdfs.support.StorageClientTemplate;
import org.danielli.xultimate.context.dfs.fastdfs.util.FastDFSUtils;
import org.danielli.xultimate.util.io.ResourceUtils;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * client test
 * 
 * @author Happy Fish / YuQing
 * @version Version 1.18
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:/dfs/fastdfs/applicationContext-service-fastdfs.xml" })
public class TestClient {
	@Resource(name = "storageClientTemplate")
	private StorageClientTemplate storageClientTemplate;

	@org.junit.Test
	public void test() {
		System.out.println("java.version=" + System.getProperty("java.version"));

		try {
			System.out.println("network_timeout=" + ClientGlobal.g_network_timeout + "ms");
			System.out.println("charset=" + ClientGlobal.g_charset);

			storageClientTemplate.execute(new StorageClientCallback() {

				@Override
				public TrackerServer getTrackerServer(TrackerClient trackerClient) throws Exception {
					return trackerClient.getConnection();
				}

				@Override
				public StorageServer getStorageServer(TrackerClient trackerClient, TrackerServer trackerServer) throws Exception {
					return null;
			  		/*
			  		storageServer = tracker.getStoreStorage(trackerServer);
			  		if (storageServer == null)
			  		{
			  			System.out.println("getStoreStorage fail, error code: " + tracker.getErrorCode());
			  			return;
			  		}
			  		*/
				}

				@Override
				public void doInStorageClient(TrackerServer trackerServer,
						StorageServer storageServer) throws Exception {
					String local_filename = ResourceUtils.getClassPathResource("/dfs/fastdfs/README").getPath();
					
					long startTime;
			  		String group_name;
			  		String remote_filename;
			  		ServerInfo[] servers;
			  		
					StorageClient client = FastDFSUtils.newStorageClient(trackerServer, storageServer);

					byte[] file_buff;
					Map<String, String> metaInformation = new HashMap<String, String>();
			  		String[] results;
			  		String master_filename;
			  		String prefix_name;
			  		String file_ext_name;
			  		String generated_slave_filename;
			  		int errno;

					metaInformation.put("width", "800");
					metaInformation.put("heigth", "600");
					metaInformation.put("bgcolor", "#FFFFFF");
					metaInformation.put("author", "Mike");

			  		file_buff = "this is a test".getBytes(ClientGlobal.g_charset);
			  		System.out.println("file length: " + file_buff.length);

					group_name = null;
					StorageServer[] storageServers = storageClientTemplate.getTrackerClient().getStoreStorages(trackerServer, group_name);
					if (storageServers == null)
					{
						System.err.println("get store storage servers fail, error code: " + storageClientTemplate.getTrackerClient().getErrorCode());
					}
					else
					{
						System.err.println("store storage servers count: " + storageServers.length);
						for (int k=0; k<storageServers.length; k++)
						{
							System.err.println((k+1) + ". " + storageServers[k].getInetSocketAddress().getAddress().getHostAddress() + ":" + storageServers[k].getInetSocketAddress().getPort());
						}
						System.err.println("");
					}
					
					startTime = System.currentTimeMillis();
					results = client.upload_file(file_buff, "txt", FastDFSUtils.newNameValuePairs(metaInformation));
					System.out.println("upload_file time used: " + (System.currentTimeMillis() - startTime) + " ms");
					/*
					 * group_name = ""; results = client.upload_file(group_name,
					 * file_buff, "txt", meta_list);
					 */
					if (results == null) {
						System.err.println("upload file fail, error code: " + client.getErrorCode());
						return;
					} else {
						group_name = results[0];
						remote_filename = results[1];
						System.err.println("group_name: " + group_name + ", remote_filename: " + remote_filename);
						System.err.println(client.get_file_info(group_name, remote_filename));

						servers = storageClientTemplate.getTrackerClient().getFetchStorages(trackerServer, group_name, remote_filename);
						if (servers == null) {
							System.err.println("get storage servers fail, error code: " + storageClientTemplate .getTrackerClient().getErrorCode());
						} else {
							System.err.println("storage servers count: " + servers.length);
							for (int k = 0; k < servers.length; k++) {
								System.err.println((k + 1) + ". " + servers[k].getIpAddr() + ":" + servers[k].getPort());
							}
							System.err.println("");
						}

						metaInformation = new HashMap<String, String>();
						metaInformation.put("width", "1024");
						metaInformation.put("heigth", "768");
						metaInformation.put("bgcolor", "#000000");
						metaInformation.put("author", "Untitle");

						startTime = System.currentTimeMillis();
						errno = client.set_metadata(group_name, remote_filename, FastDFSUtils.newNameValuePairs(metaInformation), ProtoCommon.STORAGE_SET_METADATA_FLAG_MERGE);
						System.out.println("set_metadata time used: " + (System.currentTimeMillis() - startTime) + " ms");
						if (errno == 0) {
							System.err.println("set_metadata success");
						} else {
							System.err.println("set_metadata fail, error no: " + errno);
						}

						metaInformation = FastDFSUtils.newMapFromNameValuePairs(client.get_metadata(group_name, remote_filename));
						if (metaInformation != null) {
							for (Map.Entry<String, String> entry : metaInformation.entrySet()) {
								System.out.println(entry.getKey() + " " + entry.getValue());
							}
						}

						// Thread.sleep(30000);

						startTime = System.currentTimeMillis();
						file_buff = client.download_file(group_name, remote_filename);
						System.out.println("download_file time used: " + (System.currentTimeMillis() - startTime) + " ms");

						if (file_buff != null) {
							System.out.println("file length:" + file_buff.length);
							System.out.println((new String(file_buff)));
						}

						file_buff = "this is a slave buff".getBytes(ClientGlobal.g_charset);
						master_filename = remote_filename;
						prefix_name = "-part1";
						file_ext_name = "txt";
						startTime = System.currentTimeMillis();
						results = client.upload_file(group_name, master_filename, prefix_name, file_buff, file_ext_name, FastDFSUtils.newNameValuePairs(metaInformation));
						System.out.println("upload_file time used: " + (System.currentTimeMillis() - startTime) + " ms");
						if (results != null) {
							System.err.println("slave file group_name: " + results[0] + ", remote_filename: " + results[1]);

							generated_slave_filename = ProtoCommon.genSlaveFilename(master_filename, prefix_name, file_ext_name);
							if (!generated_slave_filename.equals(results[1])) {
								System.err.println("generated slave file: " + generated_slave_filename + "\n != returned slave file: " + results[1]);
							}
							System.err.println(client.get_file_info(results[0], results[1]));
						}

						startTime = System.currentTimeMillis();
						errno = client.delete_file(group_name, remote_filename);
						System.out.println("delete_file time used: " + (System.currentTimeMillis() - startTime) + " ms");
						if (errno == 0) {
							System.err.println("Delete file success");
						} else {
							System.err.println("Delete file fail, error no: " + errno);
						}
					}

					results = client.upload_file(local_filename, null, FastDFSUtils.newNameValuePairs(metaInformation));
					if (results != null) {
						String file_id;
						int ts;
						String token;
						String file_url;
						InetSocketAddress inetSockAddr;

						group_name = results[0];
						remote_filename = results[1];
						file_id = group_name + StorageClient1.SPLIT_GROUP_NAME_AND_FILENAME_SEPERATOR + remote_filename;

						inetSockAddr = trackerServer.getInetSocketAddress();
						file_url = "http://" + inetSockAddr.getAddress().getHostAddress();
						if (ClientGlobal.g_tracker_http_port != 80) {
							file_url += ":" + ClientGlobal.g_tracker_http_port;
						}
						file_url += "/" + file_id;
						if (ClientGlobal.g_anti_steal_token) {
							ts = (int) (System.currentTimeMillis() / 1000);
							token = ProtoCommon.getToken(file_id, ts, ClientGlobal.g_secret_key);
							file_url += "?token=" + token + "&ts=" + ts;
						}

						System.err.println("group_name: " + group_name + ", remote_filename: " + remote_filename);
						System.err.println(client.get_file_info(group_name, remote_filename));
						System.err.println("file url: " + file_url);

						errno = client.download_file(group_name, remote_filename, 0, 0, "c:\\" + remote_filename.replaceAll("/", "_"));
						if (errno == 0) {
							System.err.println("Download file success");
						} else {
							System.err.println("Download file fail, error no: " + errno);
						}

						errno = client.download_file(group_name, remote_filename, 0, 0, new DownloadFileWriter("c:\\" + remote_filename.replaceAll("/", "-")));
						if (errno == 0) {
							System.err.println("Download file success");
						} else {
							System.err.println("Download file fail, error no: " + errno);
						}

						master_filename = remote_filename;
						prefix_name = "-part2";
						file_ext_name = null;
						startTime = System.currentTimeMillis();
						results = client.upload_file(group_name, master_filename, prefix_name, local_filename, null, FastDFSUtils.newNameValuePairs(metaInformation));
						System.out.println("upload_file time used: " + (System.currentTimeMillis() - startTime) + " ms");
						if (results != null) {
							System.err.println("slave file group_name: " + results[0] + ", remote_filename: " + results[1]);

							generated_slave_filename = ProtoCommon.genSlaveFilename(master_filename, prefix_name, file_ext_name);
							if (!generated_slave_filename.equals(results[1])) {
								System.err.println("generated slave file: " + generated_slave_filename + "\n != returned slave file: " + results[1]);
							}

							System.err.println(client.get_file_info(results[0], results[1]));
						}

						File f;
						f = new File(local_filename);
						int nPos = local_filename.lastIndexOf('.');
						if (nPos > 0 && local_filename.length() - nPos <= ProtoCommon.FDFS_FILE_EXT_NAME_MAX_LEN + 1) {
							file_ext_name = local_filename.substring(nPos + 1);
						} else {
							file_ext_name = null;
						}

						results = client.upload_file(null, f.length(), new UploadLocalFileSender(local_filename), file_ext_name, FastDFSUtils.newNameValuePairs(metaInformation));
						if (results != null) {
							group_name = results[0];
							remote_filename = results[1];

							System.out.println("group name: " + group_name + ", remote filename: " + remote_filename);
							System.out.println(client.get_file_info(group_name, remote_filename));

							master_filename = remote_filename;
							prefix_name = "-part3";
							startTime = System.currentTimeMillis();
							results = client.upload_file(group_name, master_filename, prefix_name, f.length(), new UploadLocalFileSender(local_filename), file_ext_name, FastDFSUtils.newNameValuePairs(metaInformation));
							System.out.println("upload_file time used: " + (System.currentTimeMillis() - startTime) + " ms");
							if (results != null) {
								System.err.println("slave file group_name: " + results[0] + ", remote_filename: " + results[1]);

								generated_slave_filename = ProtoCommon.genSlaveFilename(master_filename, prefix_name, file_ext_name);
								if (!generated_slave_filename.equals(results[1])) {
									System.err.println("generated slave file: " + generated_slave_filename + "\n != returned slave file: " + results[1]);
								}

								System.err.println(client.get_file_info(results[0], results[1]));
							}
						} else {
							System.err.println("Upload file fail, error no: " + errno);
						}

						storageServer = storageClientTemplate .getTrackerClient().getFetchStorage(trackerServer, group_name, remote_filename);
						if (storageServer == null) {
							System.out.println("getFetchStorage fail, errno code: " + storageClientTemplate.getTrackerClient().getErrorCode());
							return;
						}
						/* for test only */
						System.out.println("active test to storage server: " + ProtoCommon.activeTest(storageServer.getSocket()));
						/* for test only */
						System.out.println("active test to tracker server: " + ProtoCommon.activeTest(trackerServer.getSocket()));
					}
				}
			});
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
}