package org.danielli.xultimate.context.dfs.fastdfs;

import java.util.concurrent.ConcurrentLinkedQueue;

import javax.annotation.Resource;

import org.csource.fastdfs.ClientGlobal;
import org.csource.fastdfs.DownloadCallback;
import org.csource.fastdfs.StorageClient1;
import org.csource.fastdfs.StorageServer;
import org.csource.fastdfs.TrackerClient;
import org.csource.fastdfs.TrackerServer;
import org.danielli.xultimate.context.dfs.fastdfs.support.StorageClientTemplate;
import org.danielli.xultimate.context.dfs.fastdfs.util.FastDFSUtils;
import org.danielli.xultimate.util.thread.ThreadUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:/dfs/fastdfs/applicationContext-service-fastdfs.xml" })
public class TestLoad {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(Test.class);
	
	public static ConcurrentLinkedQueue<String> file_ids = new ConcurrentLinkedQueue<String>();
	public static int total_download_count = 0;
	public static int success_download_count = 0;
	public static int fail_download_count = 0;
	public static int total_upload_count = 0;
	public static int success_upload_count = 0;
	public static int upload_thread_count = 0;

	@Resource(name = "storageClientTemplate")
	private StorageClientTemplate storageClientTemplate;

	public class Uploader {

		public int uploadFile() throws Exception {
			return storageClientTemplate.execute(new AbstractStorageClientReturnedCallback<Integer>() {
				
				@Override
				public Integer doInStorageClient(TrackerClient trackerClient, TrackerServer trackerServer, StorageServer storageServer)
						throws Exception {
					StorageClient1 client = FastDFSUtils.newStorageClient1(trackerServer, storageServer);
					
					byte[] file_buff;
					String file_id;
					
					file_buff = new byte[2 * 1024];
					java.util.Arrays.fill(file_buff, (byte) 65);
					
					try {
						file_id = client.upload_file1(file_buff, "txt", null);
						if (file_id == null) {
							LOGGER.error("upload file fail, error code: " + client.getErrorCode());
							return -1;
						}

						TestLoad.file_ids.offer(file_id);
						return 0;
					} catch (Exception ex) {
						LOGGER.error("upload file fail, error mesg: " + ex.getMessage());
						return -1;
					}
				}
			});
		}
	}
	
	public class UploadThread extends Thread {
		private int thread_index;

		public UploadThread(int index) {
			this.thread_index = index;
		}

		public void run() {
			try {
				TestLoad.upload_thread_count++;
				Uploader uploader = new Uploader();
				
				LOGGER.info("upload thread " + this.thread_index + " start");
				
				for (int i = 0; i < 10; i++) {
					TestLoad.total_upload_count++;
					if (uploader.uploadFile() == 0) {
						TestLoad.success_upload_count++;
					}
				}
			} catch (Exception ex) {
				ex.printStackTrace();
			} finally {
				TestLoad.upload_thread_count--;
			}

			LOGGER.info("upload thread "
					+ this.thread_index + " exit, total_upload_count: "
					+ TestLoad.total_upload_count + ", success_upload_count: "
					+ TestLoad.success_upload_count
					+ ", total_download_count: "
					+ TestLoad.total_download_count
					+ ", success_download_count: "
					+ TestLoad.success_download_count);
		}
	}

	/**
	 * discard file content callback class when download file
	 * 
	 * @author Happy Fish / YuQing
	 * @version Version 1.0
	 */
	public static class DownloadFileDiscard implements DownloadCallback {
		public DownloadFileDiscard() { }

		public int recv(long file_size, byte[] data, int bytes) {
			return 0;
		}
	}
	
	public class Downloader {
		
		public int downloadFile(final String file_id) throws Exception {
			return storageClientTemplate.execute(new AbstractStorageClientReturnedCallback<Integer>() {
				
				@Override
				public Integer doInStorageClient(TrackerClient trackerClient, TrackerServer trackerServer, StorageServer storageServer)
						throws Exception {
					StorageClient1 client = FastDFSUtils.newStorageClient1(trackerServer, storageServer);
					
					int errno;
					try {
						errno = client.download_file1(file_id, new DownloadFileDiscard());
						if (errno != 0) {
							System.out.println("Download file fail, file_id: " + file_id + ", error no: " + errno);
						}
						return errno;
					} catch (Exception ex) {
						System.out.println("Download file fail, error mesg: " + ex.getMessage());
						return -1;
					}
				}
			});
		}
	}
	
	private static Integer counter_lock = new Integer(0);
	
	public class DownloadThread extends Thread {
		private int thread_index;

		public DownloadThread(int index) {
			this.thread_index = index;
		}

		public void run() {
			try {
				String file_id;
				Downloader downloader = new Downloader();

				LOGGER.info("download thread " + this.thread_index + " start");

				file_id = "";
				while (TestLoad.upload_thread_count != 0 || file_id != null) {
					file_id = (String) TestLoad.file_ids.poll();
					if (file_id == null) {
						Thread.sleep(10);
						continue;
					}

					synchronized (counter_lock) {
						TestLoad.total_download_count++;
					}
					if (downloader.downloadFile(file_id) == 0) {
						synchronized (counter_lock) {
							TestLoad.success_download_count++;
						}
					} else {
						TestLoad.fail_download_count++;
					}
				}

				for (int i = 0; i < 3 && TestLoad.total_download_count < TestLoad.total_upload_count; i++) {
					file_id = (String) TestLoad.file_ids.poll();
					if (file_id == null) {
						Thread.sleep(10);
						continue;
					}

					synchronized (counter_lock) {
						TestLoad.total_download_count++;
					}
					if (downloader.downloadFile(file_id) == 0) {
						synchronized (counter_lock) {
							TestLoad.success_download_count++;
						}
					} else {
						TestLoad.fail_download_count++;
					}
				}
			} catch (Exception ex) {
				ex.printStackTrace();
			}

			LOGGER.info("download thread " + this.thread_index
					+ " exit, total_download_count: "
					+ TestLoad.total_download_count
					+ ", success_download_count: "
					+ TestLoad.success_download_count
					+ ", fail_download_count: " + TestLoad.fail_download_count);
		}
	}
	
	@Test
	public void test() {
		LOGGER.info("network_timeout={}ms", ClientGlobal.g_network_timeout);
		LOGGER.info("charset={}", ClientGlobal.g_charset);
		try {
			for (int i = 0; i < 10; i++) {
				new UploadThread(i).start();
			}

			for (int i = 0; i < 20; i++) {
				new DownloadThread(i).start();
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		ThreadUtils.waitUntilLe(2);
	}
}