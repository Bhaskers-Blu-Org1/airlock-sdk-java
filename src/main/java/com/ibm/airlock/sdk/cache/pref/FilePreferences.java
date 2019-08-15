package com.ibm.airlock.sdk.cache.pref;

import com.ibm.airlock.sdk.cache.PersistenceEncryptor;

import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.prefs.AbstractPreferences;
import java.util.prefs.BackingStoreException;
import java.io.File;
import java.io.IOException;
import java.io.FileOutputStream;
import java.util.prefs.Preferences;

/**
 * Preferences implementation that stores to a user-defined file. See FilePreferencesFactory.
 *
 * @author David Croft (<a href="http://www.davidc.net">www.davidc.net</a>)
 * @version $Id: FilePreferences.java 283 2009-06-18 17:06:58Z david $
 */
public class FilePreferences extends AbstractPreferences {
    private static final Logger log = Logger.getLogger(FilePreferences.class.getName());

    private Map<String, String> root;
    private Map<String, FilePreferences> children;
    private File file;
    private boolean isRemoved = false;

    public Preferences node(String path) {
        return super.node(path.replace("\\","/"));
    }

    public  boolean nodeExists(String pathName) throws BackingStoreException {
        return super.nodeExists(pathName.replace("\\","/"));
    }

    public FilePreferences(AbstractPreferences parent, String name) {
        super(parent, name);

        log.log(Level.INFO, "Instantiating node " + name);
        root = new TreeMap<String, String>();
        children = new TreeMap<String, FilePreferences>();

        if (!name.isEmpty() && parent != null) {
            if (name.endsWith(".pref")) {
                this.file = new File(parent.absolutePath() + File.separator + name);
                if (!file.exists()) {
                    try {
                        if (this.file.createNewFile()) {
                            log.log(Level.INFO, "File [" + this.file.getAbsolutePath() + "] created");
                        } else {
                            log.log(Level.SEVERE, "File [" + this.file.getAbsolutePath() + "] creation failed");
                        }
                    } catch (IOException e) {
                        log.log(Level.SEVERE, "File [" + this.file.getAbsolutePath().toString() + "] creation failed, error:" + e.getMessage());
                    }
                } else {
                    log.log(Level.INFO, "File [" + this.file.getAbsolutePath() + "] already exits");
                }

                try {
                    sync();
                } catch (BackingStoreException e) {
                    log.log(Level.SEVERE, "Unable to sync on creation of node " + name, e);
                }
            } else {
                this.file = new File(parent.absolutePath() + File.separator + name);
                if (!this.file.exists()) {
                    boolean done = this.file.mkdir();
                    log.log(Level.INFO, "Folder [" + file.getAbsolutePath() + "]" + (done ? " created" : " creation failed"));
                } else {
                    log.log(Level.INFO, "Folder [" + file.getAbsolutePath() + "] folder exists");
                }
            }
        } else {
            this.file = new File("");
        }
    }


    @Override
    public String absolutePath() {
        if (file != null) {
            return file.getAbsolutePath();
        }
        return super.absolutePath();
    }

    protected void putSpi(String key, String value) {
        synchronized (root) {
            root.put(key, value);
        }
        try {
            flush();
        } catch (BackingStoreException e) {
            log.log(Level.SEVERE, "Unable to flush after putting " + key, e);
        }
    }

    @Override
    public void put(String key, String value) {
        if (key == null || value == null)
            throw new NullPointerException();

        putSpi(key, value);

    }

    protected String getSpi(String key) {
        synchronized (root) {
            return root.get(key);
        }
    }

    protected void removeSpi(String key) {
        synchronized (root) {
            root.remove(key);
        }
//        try {
//            if(!file.isDirectory()){
//                flush();
//            }
//        } catch (BackingStoreException e) {
//            log.log(Level.SEVERE, "Unable to flush after removing " + key, e);
//        }
    }

    protected void removeNodeSpi() throws BackingStoreException {
        isRemoved = true;
//        if(!file.isDirectory()){
//            flush();
//        }
    }

    protected String[] keysSpi() throws BackingStoreException {
        synchronized (root) {
            return root.keySet().toArray(new String[root.keySet().size()]);
        }
    }

    protected String[] childrenNamesSpi() throws BackingStoreException {
        return children.keySet().toArray(new String[children.keySet().size()]);
    }

    protected FilePreferences childSpi(String name) {
        FilePreferences child = children.get(name);
        if (child == null || child.isRemoved()) {
            child = new FilePreferences(this, name);
            children.put(name, child);
        }
        //clearCache();
        return child;
    }


    protected void syncSpi() throws BackingStoreException {
        if (isRemoved()) return;

        if (!file.exists()) return;

        synchronized (file) {
            Properties p = new Properties();
            try {
                p.load(PersistenceEncryptor.decryptAES(file));

                StringBuilder sb = new StringBuilder();
                getPath(sb);
                String path = sb.toString();

                final Enumeration<?> pnen = p.propertyNames();
                while (pnen.hasMoreElements()) {
                    String propKey = (String) pnen.nextElement();
                    if (propKey.startsWith(path)) {
                        String subKey = propKey.substring(path.length());
                        // Only load immediate descendants
                        synchronized (root) {
                            root.put(subKey, p.getProperty(propKey));
                        }

                    }
                }
            } catch (IOException e) {
                throw new BackingStoreException(e);
            }
        }
    }

    private void getPath(StringBuilder sb) {
        final FilePreferences parent = (FilePreferences) parent();
        if (parent == null) return;

        parent.getPath(sb);
        sb.append(name()).append('.');
    }


    public void removeNode() throws BackingStoreException {
        file.delete();
        children.clear();
        super.removeNode();
    }

    protected void flushSpi() throws BackingStoreException {
        synchronized (file) {
            Properties p = new Properties();
            try {

                StringBuilder sb = new StringBuilder();
                getPath(sb);
                String path = sb.toString();

                if (file.exists()) {
                    try {
                        p.load(PersistenceEncryptor.decryptAES(file));
                    }catch (Exception e){
                        throw new BackingStoreException(e);
                    }

                    List<String> toRemove = new ArrayList<String>();

                    // Make a list of all direct children of this node to be removed
                    final Enumeration<?> pnen = p.propertyNames();
                    while (pnen.hasMoreElements()) {
                        String propKey = (String) pnen.nextElement();
                        if (propKey.startsWith(path)) {
                            String subKey = propKey.substring(path.length());
                            // Only do immediate descendants
                            toRemove.add(propKey);

                        }
                    }

                    // Remove them now that the enumeration is done with
                    for (String propKey : toRemove) {
                        p.remove(propKey);
                    }
                }

                synchronized (root) {
                    // If this node hasn't been removed, add back in any values
                    if (!isRemoved) {
                        Set<String> keySet = new HashSet(root.keySet());
                        for (String s : keySet) {
                            p.setProperty(path + s, root.get(s));
                        }
                    }
                }

                FileOutputStream fo = new FileOutputStream(file);
                p.store(fo, "FilePreferences");
                fo.close();
                PersistenceEncryptor.encryptAES(file);
            } catch (IOException e) {
                throw new BackingStoreException(e);
            }
        }
    }
}
