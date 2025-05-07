import geopandas as gpd

gdf = gpd.read_file("viasciclistas23_simple.geojson")

gdf['geometry'] = gdf['geometry'].simplify(tolerance=0.0001, preserve_topology=True)

gdf = gdf[['geometry']]

gdf.to_file("viasciclistas23_simple.geojson", driver="GeoJSON")
