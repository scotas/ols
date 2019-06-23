package com.scotas.lucene.index;

import java.sql.SQLException;

import java.util.Random;


/**
 */
public class TestDBIndexParallel extends DBTestCase {
    private static int MAX_ROWS = 20000;
    private static int BATCH_SIZE = Integer.parseInt(System.getProperty("db.batch-size", "50"));
    private static int MAX_THREADS = 3;
    private static int NUM_INSERTS = 20;
    private static int NUM_DELETES = 10;
    private static int NUM_UPDATES = 10;
    private static int NUM_READS = Math.max(Math.max(NUM_INSERTS, NUM_DELETES),NUM_UPDATES) * MAX_THREADS * 10;
    // array of max(NUM_INSERTS,NUM_DELETES,NUM_UPDATES) size
    private static int[] RANDOM_BLOCKS = new int[Math.max(Math.max(NUM_INSERTS, NUM_DELETES),NUM_UPDATES)];
    private Thread[] ti = new Thread[MAX_THREADS];
    private Thread[] td = new Thread[MAX_THREADS];
    private Thread[] ts = new Thread[MAX_THREADS];
    private Thread[] tu = new Thread[MAX_THREADS];
    private Thread[] tc = new Thread[MAX_THREADS];
    private Thread[] to = new Thread[1];
    private static final Random RANDOM = new Random();
    private boolean onLine = true;
    private boolean continueSync = true;
    private boolean bufferedQueue = false;
    
    private static int random(int i) { // for JDK 1.1 compatibility
        int r = RANDOM.nextInt();
        if (r < 0)
            r = -r;
        return r % i;
    }

    static {
      // initialize blocks for testing, same for both
      for (int i = 0; i < RANDOM_BLOCKS.length; i++)
          RANDOM_BLOCKS[i] = BATCH_SIZE * random(MAX_ROWS/BATCH_SIZE);
    }
  
    public TestDBIndexParallel() throws SQLException {
        super();
    }

    public void setUp() throws SQLException {
        createTable();
    }

    public void testLuceneDomainIndex() throws SQLException,
                                               InterruptedException {
        luceneMode = true;
        onLine = "true".equalsIgnoreCase(System.getProperty("db.online-mode", "true"));
        createIndex(onLine);
        System.out.println("OnLine: " + onLine);
        for (int i = 0; i < MAX_THREADS; i++) {
            ti[i] = new Thread("Inserter " + i) {
                    public void run() {
                        long elapsedTime = 0;
                        for (int j = 0; j < NUM_INSERTS; j++)
                            try {
                                int waitTime = random(10);
                                sleep(1000 * waitTime);
                                //System.out.println(this +
                                //                   " inserting at block " +
                                //                   block);
                                long stepTime = insertRows(RANDOM_BLOCKS[j], RANDOM_BLOCKS[j] +
                                                           BATCH_SIZE - 1);
                                elapsedTime += stepTime;
                                        
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                                throw new RuntimeException(e);
                            } catch (SQLException e) {
                                e.printStackTrace();
                            }
                        System.out.println("Avg time on Lucene inserts: " +
                                           (elapsedTime / NUM_INSERTS));
                    }

                    public String toString() {
                        return getName();
                    }
                };
            ti[i].start();
            td[i] = new Thread("Deleter " + i) {
                    public void run() {
                        long elapsedTime = 0;
                        for (int j = 0; j < NUM_DELETES; j++)
                            try {
                                int waitTime = random(10);
                                sleep(1000 * waitTime);
                                //System.out.println(this +
                                //                   " deleting at block " +
                                //                   block);
                                elapsedTime +=
                                        deleteRows(RANDOM_BLOCKS[j], RANDOM_BLOCKS[j] +
                                                   BATCH_SIZE - 1);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                                throw new RuntimeException(e);
                            } catch (SQLException e) {
                                e.printStackTrace();
                            }
                        System.out.println("Avg time on Lucene deletes: " +
                                           (elapsedTime / NUM_DELETES));
                    }

                    public String toString() {
                        return getName();
                    }
                };
            td[i].start();
            tu[i] = new Thread("Updater " + i) {
                    public void run() {
                        long elapsedTime = 0;
                        for (int j = 0; j < NUM_UPDATES; j++)
                            try {
                                int waitTime = random(10);
                                sleep(1000 * waitTime);
                                //System.out.println(this +
                                //                   " updating at block " +
                                //                   block);
                                elapsedTime +=
                                        updateRows(RANDOM_BLOCKS[j], RANDOM_BLOCKS[j] +
                                                   BATCH_SIZE - 1);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                                throw new RuntimeException(e);
                            } catch (SQLException e) {
                                e.printStackTrace();
                            }
                        System.out.println("Avg time on Lucene updates: " +
                                           (elapsedTime / NUM_UPDATES));
                    }

                    public String toString() {
                        return getName();
                    }
                };
            tu[i].start();
            ts[i] = new Thread("Searcher " + i) {
                    public void run() {
                        long elapsedTime = 0;
                        for (int j = 0; j < NUM_READS; j++)
                            try {
                                int waitTime = random(10);
                                int row = random(MAX_ROWS);
                                sleep(100 * waitTime);
                                //System.out.println(this +
                                //                   " searching row " +
                                //                   row);
                                elapsedTime += findRows(row, BATCH_SIZE);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                                throw new RuntimeException(e);
                            } catch (SQLException e) {
                                e.printStackTrace();
                            }
                        System.out.println("Avg time on Lucene search: " +
                                           (elapsedTime / NUM_READS));
                    }

                    public String toString() {
                        return getName();
                    }
                };
            ts[i].start();
            tc[i] = new Thread("Hits " + i) {
                    public void run() {
                        long elapsedTime = 0;
                        for (int j = 0; j < NUM_READS; j++)
                            try {
                                int waitTime = random(10);
                                int row = random(MAX_ROWS);
                                sleep(100 * waitTime);
                                //System.out.println(this +
                                //                   " count hits " +
                                //                   row);
                                elapsedTime += countHits(row);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                                throw new RuntimeException(e);
                            } catch (SQLException e) {
                                e.printStackTrace();
                            }
                        System.out.println("Avg time on Lucene count hits: " +
                                           (elapsedTime / NUM_READS));
                    }

                    public String toString() {
                        return getName();
                    }
                };
            tc[i].start();
        }
        if (!onLine) {
            to[0] = new Thread("Sync ") {
                    public void run() {
                        long elapsedTime = 0;
                        while (continueSync)
                            try {
                                sleep(5000); // Sync every 10 second
                                elapsedTime += syncIndex();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                                throw new RuntimeException(e);
                            } catch (SQLException e) {
                                e.printStackTrace();
                            }
                        System.out.println("Total time on Lucene sync: " +
                                           elapsedTime);
                    }

                    public String toString() {
                        return getName();
                    }
                };
            to[0].start();
        }
        for (int i = 0; i < MAX_THREADS; i++)
            ti[i].join();
        for (int i = 0; i < MAX_THREADS; i++)
            tu[i].join();
        for (int i = 0; i < MAX_THREADS; i++)
            td[i].join();
        for (int i = 0; i < MAX_THREADS; i++)
            ts[i].join();
        for (int i = 0; i < MAX_THREADS; i++)
            tc[i].join();
        if (!onLine) {
            continueSync = false; // signal Sync thread to stop
            to[0].join(); // wait last sync to finish
        }
        dropIndex();
    }

    public void testTextDomainIndex() throws SQLException,
                                             InterruptedException {
        luceneMode = false;
        onLine = "true".equalsIgnoreCase(System.getProperty("db.online-mode", "true"));
        createIndex(onLine);
        System.out.println("OnLine: " + onLine);
        for (int i = 0; i < MAX_THREADS; i++) {
            ti[i] = new Thread("Inserter " + i) {
                    public void run() {
                        long elapsedTime = 0;
                        for (int j = 0; j < NUM_INSERTS; j++)
                            try {
                                int waitTime = random(10);
                                sleep(1000 * waitTime);
                                //System.out.println(this +
                                //                   " inserting at block " +
                                //                   block);
                                elapsedTime +=
                                        insertRows(RANDOM_BLOCKS[j], RANDOM_BLOCKS[j] +
                                                   BATCH_SIZE - 1);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                                throw new RuntimeException(e);
                            } catch (SQLException e) {
                                e.printStackTrace();
                            }
                        System.out.println("Avg time on Text inserts: " +
                                           (elapsedTime / NUM_INSERTS));
                    }

                    public String toString() {
                        return getName();
                    }
                };
            ti[i].start();
            td[i] = new Thread("Deleter " + i) {
                    public void run() {
                        long elapsedTime = 0;
                        for (int j = 0; j < NUM_DELETES; j++)
                            try {
                                int waitTime = random(10);
                                sleep(1000 * waitTime);
                                //System.out.println(this +
                                //                   " deleting at block " +
                                //                   block);
                                elapsedTime +=
                                        deleteRows(RANDOM_BLOCKS[j], RANDOM_BLOCKS[j] +
                                                   BATCH_SIZE - 1);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                                throw new RuntimeException(e);
                            } catch (SQLException e) {
                                e.printStackTrace();
                            }
                        System.out.println("Avg time on Text deletes: " +
                                           (elapsedTime / NUM_DELETES));
                    }

                    public String toString() {
                        return getName();
                    }
                };
            td[i].start();
            tu[i] = new Thread("Updater " + i) {
                    public void run() {
                        long elapsedTime = 0;
                        for (int j = 0; j < NUM_UPDATES; j++)
                            try {
                                int waitTime = random(10);
                                sleep(1000 * waitTime);
                                //System.out.println(this +
                                //                   " updating at block " +
                                //                   block);
                                elapsedTime +=
                                        updateRows(RANDOM_BLOCKS[j], RANDOM_BLOCKS[j] +
                                                   BATCH_SIZE - 1);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                                throw new RuntimeException(e);
                            } catch (SQLException e) {
                                e.printStackTrace();
                            }
                        System.out.println("Avg time on Text updates: " +
                                           (elapsedTime / NUM_UPDATES));
                    }

                    public String toString() {
                        return getName();
                    }
                };
            tu[i].start();
            ts[i] = new Thread("Searcher " + i) {
                    public void run() {
                        long elapsedTime = 0;
                        for (int j = 0; j < NUM_READS; j++)
                            try {
                                int waitTime = random(10);
                                int row = random(1000);
                                sleep(100 * waitTime);
                                //System.out.println(this +
                                //                   " searching row " +
                                //                   row);
                                elapsedTime += findRows(row, BATCH_SIZE);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                                throw new RuntimeException(e);
                            } catch (SQLException e) {
                                e.printStackTrace();
                            }
                        System.out.println("Avg time on Text search: " +
                                           (elapsedTime / NUM_READS));
                    }

                    public String toString() {
                        return getName();
                    }
                };
            ts[i].start();
            tc[i] = new Thread("Hits " + i) {
                    public void run() {
                        long elapsedTime = 0;
                        for (int j = 0; j < NUM_READS; j++)
                            try {
                                int waitTime = random(10);
                                int row = random(1000);
                                sleep(100 * waitTime);
                                //System.out.println(this +
                                //                   " count hits " +
                                //                   row);
                                elapsedTime += countHits(row);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                                throw new RuntimeException(e);
                            } catch (SQLException e) {
                                e.printStackTrace();
                            }
                        System.out.println("Avg time on Text count hits: " +
                                           (elapsedTime / NUM_READS));
                    }

                    public String toString() {
                        return getName();
                    }
                };
            tc[i].start();
        }
        if (!onLine) {
            to[0] = new Thread("Sync ") {
                    public void run() {
                        long elapsedTime = 0;
                        while (continueSync)
                            try {
                                sleep(5000); // Sync every 5 second
                                elapsedTime += syncIndex();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                                throw new RuntimeException(e);
                            } catch (SQLException e) {
                                e.printStackTrace();
                            }
                        System.out.println("Total time on Text sync: " +
                                           elapsedTime);
                    }

                    public String toString() {
                        return getName();
                    }
                };
            to[0].start();
        }
        for (int i = 0; i < MAX_THREADS; i++)
            ti[i].join();
        for (int i = 0; i < MAX_THREADS; i++)
            tu[i].join();
        for (int i = 0; i < MAX_THREADS; i++)
            td[i].join();
        for (int i = 0; i < MAX_THREADS; i++)
            ts[i].join();
        for (int i = 0; i < MAX_THREADS; i++)
            tc[i].join();
        if (!onLine) {
            continueSync = false; // signal Sync thread to stop
            to[0].join(); // wait last sync to finish
        }
        dropIndex();
    }

    public void tearDown() throws SQLException {
        dropTable();
    }
}


