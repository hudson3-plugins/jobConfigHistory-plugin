/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package hudson.model;

import hudson.XmlFile;
import hudson.model.listeners.SaveableListener;
import hudson.security.Permission;
import hudson.util.AtomicFileWriter;
import hudson.util.IOException2;
import java.io.IOException;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

/**
 *
 * @author Bob Foster
 */
public class AbstractItemHelper {

    public static void updateByXml(AbstractItem item, StreamSource source) throws IOException {
        updateByXml(item, (Source)source);
    }

    /**
     * Updates Job by its XML definition.
     */
    public static void updateByXml(AbstractItem item, Source source) throws IOException {
        item.checkPermission(Permission.CONFIGURE);
        XmlFile configXmlFile = item.getConfigFile();
        AtomicFileWriter out = new AtomicFileWriter(configXmlFile.getFile());
        try {
            try {
                // this allows us to use UTF-8 for storing data,
                // plus it checks any well-formedness issue in the submitted
                // data
                Transformer t = TransformerFactory.newInstance()
                        .newTransformer();
                t.transform(source,
                        new StreamResult(out));
                out.close();
            } catch (TransformerException e) {
                throw new IOException2("Failed to persist configuration.xml", e);
            }

            // try to reflect the changes by reloading
            new XmlFile(Items.XSTREAM, out.getTemporaryFile()).unmarshal(item);
            //Items.updatingByXml.set(true);
            try {
                item.onLoad(item.getParent(), item.getRootDir().getName());
            } finally {
                //Items.updatingByXml.set(false);
            }
            Hudson.getInstance().rebuildDependencyGraph();

            // if everything went well, commit this new version
            out.commit();
            SaveableListener.fireOnChange(item, item.getConfigFile());
        } finally {
            out.abort(); // don't leave anything behind
        }
    }


}
