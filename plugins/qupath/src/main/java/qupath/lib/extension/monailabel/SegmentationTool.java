package qupath.lib.extension.monailabel;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.scene.input.MouseEvent;
import qupath.lib.extension.monailabel.MonaiLabelClient.ResponseInfo;
import qupath.lib.extension.monailabel.MonaiLabelClient.ImageInfo;
import qupath.lib.extension.monailabel.commands.RunInference;
import qupath.lib.extension.monailabel.commands.utils.NucleiAnnotations;
import qupath.lib.extension.monailabel.commands.utils.StringGenerator;
import qupath.lib.gui.dialogs.Dialogs;
import qupath.lib.gui.viewer.tools.RectangleTool;
import qupath.lib.objects.PathObject;
import qupath.lib.plugins.parameters.ParameterList;
import qupath.lib.roi.RectangleROI;
import qupath.lib.roi.interfaces.ROI;

public class SegmentationTool extends RectangleTool {
  private final static Logger logger = LoggerFactory.getLogger(SegmentationTool.class);

  public void mouseReleased(MouseEvent e) {
    String selectedModel = null;
    int selectedTileSize = 1024;
    ResponseInfo info = null;
    Path annotationXML = null;

    var viewer = getViewer();
    if (viewer == null || viewer.getImageData() == null) {
      return;
    }
    logger.info("+++++++ Segmentation Tool... new ROI created...");
    super.mouseReleased(e);

    var imageData = viewer.getImageData();
    PathObject selectedObject = viewer.getSelectedObject();
    if (selectedObject == null)
      return;

    ROI roi = selectedObject.getROI();
    if (roi == null || !(roi instanceof RectangleROI) || roi.isEmpty())
      return;

    try {
      String imageFile = Utils.getFileName(viewer.getImageData().getServerPath());
      String imageName = StringGenerator.generateName();
      String im = imageFile.toLowerCase();
      boolean isWSI = (im.endsWith(".png") || im.endsWith(".jpg") || im.endsWith(".jpeg")) ? false : true;
      logger.info("MONAILabel:: isWSI: " + isWSI + "; File: " + imageFile);

      if (info == null) {
        info = MonaiLabelClient.info();
        List<String> names = new ArrayList<String>();
        for (String n : info.models.keySet()) {
          logger.info("Model: " + n + "; Type: " + info.models.get(n).type);
          if (info.models.get(n).type.equalsIgnoreCase("segmentation")) {
            names.add(n);
          }
        }
        int tileSize = selectedTileSize;
        if (names.size() == 0) {
          return;
        }
        if (names.size() == 1) {
          selectedModel = names.get(0);
        }

        if (selectedModel == null || selectedModel.isEmpty()) {
          ParameterList list = new ParameterList();
          list.addChoiceParameter("Model", "Model Name", names.get(0), names);
          if (isWSI) {
            list.addIntParameter("TileSize", "TileSize", tileSize);
          }

          if (!Dialogs.showParameterDialog("MONAILabel", list)) {
            return;
          }

          selectedModel = (String) list.getChoiceParameterValue("Model");
          selectedTileSize = isWSI ? list.getIntParameterValue("TileSize").intValue() : tileSize;
        }
      }

      if (selectedModel.isBlank()) {
        return;
      }

      int[] bbox = Utils.getBBOX(roi);
      if (isWSI) {
        annotationXML = NucleiAnnotations.getAnnotationsXml(imageName, imageData, bbox);
        if (annotationXML != null) {
          Path imagePatch = java.nio.file.Files.createTempFile("patch", ".png");
          String patchName = isWSI ? imageName + String.format("-patch-%d_%d_%d_%d", bbox[0], bbox[1], bbox[2], bbox[3])
              : imageName;
          ImageInfo imageInfo = MonaiLabelClient.saveImage(patchName, imagePatch.toFile(), "{}");
          MonaiLabelClient.saveLabel(imageInfo.image, annotationXML.toFile(), null, "{}");
        }
      }
      RunInference.runInference(selectedModel, info, bbox, selectedTileSize, viewer.getImageData(), imageFile,
          isWSI, imageName);
    } catch (Exception ex) {
      ex.printStackTrace();
      Dialogs.showErrorMessage("MONAILabel", ex);
    }
  }
}
