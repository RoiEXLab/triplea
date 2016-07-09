package games.strategy.triplea.ui.screen.drawable;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.triplea.attachments.TerritoryAttachment;
import games.strategy.triplea.formatter.MyFormatter;
import games.strategy.triplea.image.MapImage;
import games.strategy.triplea.ui.IUIContext;
import games.strategy.triplea.ui.mapdata.MapData;

import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.util.List;
import java.util.Optional;

class TerritoryNameDrawable implements IDrawable {
  private final String m_territoryName;
  private final IUIContext m_uiContext;

  public TerritoryNameDrawable(final String territoryName, final IUIContext context) {
    this.m_territoryName = territoryName;
    this.m_uiContext = context;
  }

  @Override
  public void draw(final Rectangle bounds, final GameData data, final Graphics2D graphics, final MapData mapData,
      final AffineTransform unscaled, final AffineTransform scaled) {
    final Territory territory = data.getMap().getTerritory(m_territoryName);
    final TerritoryAttachment ta = TerritoryAttachment.get(territory);
    final boolean drawFromTopLeft = mapData.drawNamesFromTopLeft();
    final boolean showSeaNames = mapData.drawSeaZoneNames();
    final boolean showComments = mapData.drawComments();
    boolean drawComments = false;
    String commentText = null;
    if (territory.isWater()) {
      // this is for special comments, like convoy zones, etc.
      if (ta != null && showComments) {
        if (ta.getConvoyRoute() && ta.getProduction() > 0 && ta.getOriginalOwner() != null) {
          drawComments = true;
          if (ta.getConvoyAttached().isEmpty()) {
            commentText = MyFormatter
                .defaultNamedToTextList(TerritoryAttachment.getWhatTerritoriesThisIsUsedInConvoysFor(territory, data))
                + " " + ta.getOriginalOwner().getName() + " Blockade Route";
          } else {
            commentText = MyFormatter.defaultNamedToTextList(ta.getConvoyAttached()) + " "
                + ta.getOriginalOwner().getName() + " Convoy Route";
          }
        } else if (ta.getConvoyRoute()) {
          drawComments = true;
          if (ta.getConvoyAttached().isEmpty()) {
            commentText = MyFormatter.defaultNamedToTextList(
                TerritoryAttachment.getWhatTerritoriesThisIsUsedInConvoysFor(territory, data)) + " Blockade Route";
          } else {
            commentText = MyFormatter.defaultNamedToTextList(ta.getConvoyAttached()) + " Convoy Route";
          }
        } else if (ta.getProduction() > 0 && ta.getOriginalOwner() != null) {
          drawComments = true;
          final PlayerID originalOwner = ta.getOriginalOwner();
          commentText = originalOwner.getName() + " Convoy Center";
        }
      }
      if (!drawComments && !showSeaNames) {
        return;
      }
    }

    graphics.setFont(MapImage.getPropertyMapFont());
    graphics.setColor(MapImage.getPropertyTerritoryNameAndPUAndCommentcolor());
    final FontMetrics fm = graphics.getFontMetrics();

    // if we specify a placement point, use it otherwise try to center it
    int x;
    int y;
    final Optional<Point> namePlace = mapData.getNamePlacementPoint(territory);
    if (namePlace.isPresent()) {
      x = namePlace.get().x;
      y = namePlace.get().y;
    } else {
      final Rectangle territoryBounds = getBestTerritoryNameRect(mapData, territory, fm);
      x = territoryBounds.x + (int) territoryBounds.getWidth() / 2 - fm.stringWidth(territory.getName()) / 2;
      y = territoryBounds.y + (int) territoryBounds.getHeight() / 2 + fm.getAscent() / 2;
    }

    // draw comments above names
    if (showComments && drawComments && commentText != null) {
      final Optional<Point> place = mapData.getCommentMarkerLocation(territory);
      if (place.isPresent()) {
        draw(bounds, graphics, place.get().x, place.get().y, null, commentText, drawFromTopLeft);
      } else {
        draw(bounds, graphics, x, y - fm.getHeight(), null, commentText, drawFromTopLeft);
      }
    }
    // draw territory names
    if (mapData.drawTerritoryNames() && mapData.shouldDrawTerritoryName(m_territoryName)) {
      if (!territory.isWater() || showSeaNames) {
        final Image nameImage = mapData.getTerritoryNameImages().get(territory.getName());
        draw(bounds, graphics, x, y, nameImage, territory.getName(), drawFromTopLeft);
      }
    }
    // draw the PUs.
    if (ta != null && ta.getProduction() > 0 && mapData.drawResources()) {
      final Image img = m_uiContext.getPUImageFactory().getPUImage(ta.getProduction());
      final String prod = Integer.valueOf(ta.getProduction()).toString();
      final Optional<Point> place = mapData.getPUPlacementPoint(territory);
      // if pu_place.txt is specified draw there
      if (place.isPresent()) {
        draw(bounds, graphics, place.get().x, place.get().y, img, prod, drawFromTopLeft);
      } else {
        // otherwise, draw under the territory name
        draw(bounds, graphics, x + ((fm.stringWidth(m_territoryName)) >> 1) - ((fm.stringWidth(prod)) >> 1),
            y + fm.getLeading() + fm.getAscent(), img, prod, drawFromTopLeft);
      }
    }
  }

  /**
   * Find the best rectangle inside the territory to place the name in. Finds the rectangle
   * that can fit the name, that is the closest to the vertical center, and has a large width at
   * that location. If there isn't any rectangles that can fit the name then default back to the
   * bounding rectangle.
   */
  private Rectangle getBestTerritoryNameRect(final MapData mapData, final Territory territory,
      final FontMetrics fontMetrics) {

    // Find bounding rectangle and parameters for creating a grid (20 x 20) across the territory
    final Rectangle territoryBounds = mapData.getBoundingRect(territory);
    Rectangle result = territoryBounds;
    final int maxX = territoryBounds.x + territoryBounds.width;
    final int maxY = territoryBounds.y + territoryBounds.height;
    final int centerY = territoryBounds.y + territoryBounds.height / 2;
    final int incrementX = (int) Math.ceil(territoryBounds.width / 20.0);
    final int incrementY = (int) Math.ceil(territoryBounds.height / 20.0);
    final int nameWidth = fontMetrics.stringWidth(territory.getName());
    final int nameHeight = fontMetrics.getAscent();
    int maxScore = 0;

    // Loop through the grid moving the starting point and determining max width at that point
    for (int x = territoryBounds.x; x < maxX - nameWidth; x += incrementX) {
      for (int y = territoryBounds.y; y < maxY - nameHeight; y += incrementY) {
        for (int endX = maxX; endX > x; endX -= incrementX) {
          final Rectangle rectangle = new Rectangle(x, y, endX - x, nameHeight);

          // Ranges from 0 when at very top or bottom of territory to height/2 when at vertical center
          final int verticalDistanceFromEdge = territoryBounds.height / 2 - Math.abs(centerY - nameHeight - y);

          // Score rectangle based on how close to vertical center and territory width at location
          final int score = verticalDistanceFromEdge * rectangle.width;
          if (rectangle.width > nameWidth && score > maxScore) {

            // Check to make sure rectangle is contained in the territory
            if (isRectangleContainedInTerritory(rectangle, territory, mapData)) {
              maxScore = score;
              result = rectangle;
              break;
            }
          }
        }
      }
    }
    return result;
  }

  private boolean isRectangleContainedInTerritory(final Rectangle rectangle, final Territory territory,
      final MapData mapData) {
    final List<Polygon> polygons = mapData.getPolygons(territory.getName());
    for (final Polygon polygon : polygons) {
      if (polygon.contains(rectangle)) {
        return true;
      }
    }
    return false;
  }

  private void draw(final Rectangle bounds, final Graphics2D graphics, final int x, final int y, final Image img,
      final String prod, final boolean drawFromTopLeft) {
    int yNormal = y;
    if (img == null) {
      if (graphics.getFont().getSize() <= 0) {
        return;
      }
      if (drawFromTopLeft) {
        final FontMetrics fm = graphics.getFontMetrics();
        yNormal += fm.getHeight();
      }
      graphics.drawString(prod, x - bounds.x, yNormal - bounds.y);
    } else {
      // we want to be consistent
      // drawString takes y as the base line position
      // drawImage takes x as the top right corner
      if (!drawFromTopLeft) {
        yNormal -= img.getHeight(null);
      }
      graphics.drawImage(img, x - bounds.x, yNormal - bounds.y, null);
    }
  }

  @Override
  public int getLevel() {
    return TERRITORY_TEXT_LEVEL;
  }
}
