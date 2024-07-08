from pathlib import Path
import os
import shutil

base_path = Path(__file__).parent.parent
dist_path = base_path.joinpath("dist")

shutil.rmtree(dist_path, ignore_errors=True)
os.makedirs(dist_path, exist_ok=True)

configs = base_path.joinpath("configs")
shutil.copytree(configs, dist_path.joinpath("configs"))

jar_path = base_path.parent.joinpath(
    "target", "byzcast-tcc-1.0-SNAPSHOT-jar-with-dependencies.jar"
)
shutil.copy(jar_path, dist_path.joinpath("byzcast-tcc.jar"))

json_path = base_path.joinpath("config.json")
shutil.copy(json_path, dist_path.joinpath("config.json"))
