package qupath.lib.extension.monailabel.commands.utils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;

import java.awt.image.BufferedImage;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import qupath.lib.extension.monailabel.commands.RunInference;
import qupath.lib.images.ImageData;
import qupath.lib.objects.PathCellObject;
import qupath.lib.objects.PathObject;
import qupath.lib.roi.ROIs;
import qupath.lib.roi.interfaces.ROI;

public class NucleiAnnotations {

  private final static Logger logger = LoggerFactory.getLogger(RunInference.class);

  public static Path getAnnotationsXml(String image, ImageData<BufferedImage> imageData, int[] bbox)
      throws IOException, ParserConfigurationException, TransformerException {
    DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
    DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

    // root elements
    Document doc = docBuilder.newDocument();
    Element rootElement = doc.createElement("ASAP_Annotations");
    doc.appendChild(rootElement);

    Element annotations = doc.createElement("Annotations");
    annotations.setAttribute("Name", "");
    annotations.setAttribute("Description", "");
    annotations.setAttribute("X", String.valueOf(bbox[0]));
    annotations.setAttribute("Y", String.valueOf(bbox[1]));
    annotations.setAttribute("W", String.valueOf(bbox[2]));
    annotations.setAttribute("H", String.valueOf(bbox[3]));
    rootElement.appendChild(annotations);

    ROI patchROI = (bbox[2] > 0 && bbox[3] > 0) ? ROIs.createRectangleROI(bbox[0], bbox[1], bbox[2], bbox[3], null)
        : null;

    int count = 0;
    var groups = new HashMap<String, String>();
    List<PathObject> objs = imageData.getHierarchy().getFlattenedObjectList(null);
    for (int i = 0; i < objs.size(); i++) {
      var a = objs.get(i);

      // Ignore which doesn't have class
      String name = a.getPathClass() != null ? a.getPathClass().getName() : null;
      if (name == null || name.isEmpty()) {
        continue;
      }

      var roi = a.getROI();
      if (a.isCell()) {
        roi = ((PathCellObject) a).getNucleusROI();
      }

      // Ignore Points
      if (roi == null || roi.isPoint()) {
        continue;
      }

      // Ignore other objects not part of BBOX
      if (patchROI != null && !patchROI.contains(roi.getCentroidX(), roi.getCentroidY())) {
        continue;
      }

      var points = roi.getAllPoints();
      var color = String.format("#%06x", 0xFFFFFF & a.getPathClass().getColor());
      groups.put(name, color);

      Element annotation = doc.createElement("Annotation");
      annotation.setAttribute("Name", name);
      annotation.setAttribute("Type", roi.getRoiName());
      annotation.setAttribute("PartOfGroup", name);
      annotation.setAttribute("Color", color);
      annotations.appendChild(annotation);

      Element coordinates = doc.createElement("Coordinates");
      annotation.appendChild(coordinates);

      for (int j = 0; j < points.size(); j++) {
        var p = points.get(j);
        Element coordinate = doc.createElement("Coordinate");
        coordinate.setAttribute("Order", String.valueOf(j));
        coordinate.setAttribute("X", String.valueOf((int) p.getX() - bbox[0]));
        coordinate.setAttribute("Y", String.valueOf((int) p.getY() - bbox[1]));
        coordinates.appendChild(coordinate);
      }
      count++;
    }

    Element annotationGroups = doc.createElement("AnnotationGroups");
    rootElement.appendChild(annotationGroups);

    for (String group : groups.keySet()) {
      Element annotationGroup = doc.createElement("Group");
      annotationGroup.setAttribute("Name", group);
      annotationGroup.setAttribute("PartOfGroup", "None");
      annotationGroup.setAttribute("Color", groups.get(group));
      annotationGroups.appendChild(annotationGroup);
    }

    logger.info("Total Objects saved: " + count);
    if (count != 0) {
      return writeXml(image, doc);
    }
    return null;
  }

  private static Path writeXml(String image, Document doc) throws TransformerException, IOException {
    FileOutputStream output = null;
    try {
      var path = java.nio.file.Files.createTempFile(image, ".xml");
      output = new FileOutputStream(path.toFile());

      TransformerFactory transformerFactory = TransformerFactory.newInstance();
      Transformer transformer = transformerFactory.newTransformer();

      // pretty print
      transformer.setOutputProperty(OutputKeys.INDENT, "yes");

      DOMSource source = new DOMSource(doc);
      StreamResult result = new StreamResult(output);
      transformer.transform(source, result);
      output.close();
      return path;
    } finally {
      if (output != null) {
        output.close();
      }
    }
  }

}