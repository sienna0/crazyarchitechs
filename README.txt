FrogToGrapher - Beta Release README.txt
Team 3 Crazy Architechs

Contributors
Sienna Li (sl3493)
Nigel Tatem (nt387)
Arno Huet (ah2276)
Jazzlin Yee (jjy42)
Ore Adeniyi (oaa42)
Nicholas Letendre (nzl3)

Overview
Frogtographer is a puzzle platformer where Zuko, a frog photographer, uses her magical camera to
transfer physical properties between objects and solve levels. Take a photo of a honey block to
capture its stickiness, then stick that photo onto another object to change how it behaves.
Reach the goal door at the end of each level.

Running the Game
Choose the JAR for your platform and double-click it, or run from a terminal:

    java -jar Physics-1.0.0-mac.jar      (macOS)
    java -jar Physics-1.0.0-win.jar      (Windows)
    java -jar Physics-1.0.0-linux.jar    (Linux)

Requires Java 17 or later.

Features Implemented
- Player movement and jumping
- Three level designs with varying difficulty
- Photo taking and sticking
- Object physics interactions (honey, ice, cloud)
- Fly collectibles hidden throughout levels
- Parallax scrolling background
- Pause and options menus

Object Types
- Honey  - sticky surface, slows horizontal movement
- Ice    - frictionless, objects slide freely
- Cloud  - floats upward, cancels gravity on the target

Controls
A / D or Left / Right Arrow Keys    - Move left and right
W, Space, or Up Arrow               - Jump
Mouse                               - Aim crosshair
Left Click (no photo selected)      - Take photo of object in range
Left Click (photo selected)         - Stick photo onto object in range
Right Click                         - Remove photo from object
Q                                   - Drop selected photo
1 / 2 / 3 or click slot            - Select inventory slot
Tab                                 - Toggle range indicators
Left Click on fly                   - Collect fly with Zuko's tongue

Known Bugs
- Zuko may be able to cling to the side of a rotated object
- Photos may not move perfectly with a moving object at times
