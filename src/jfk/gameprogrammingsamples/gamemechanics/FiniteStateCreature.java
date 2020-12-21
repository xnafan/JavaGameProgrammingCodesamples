package jfk.gameprogrammingsamples.gamemechanics;

import java.awt.*;
import java.awt.geom.Dimension2D;
import java.util.ArrayList;
import java.util.Random;

//simple sprite class for storing an image at a location and drawing it centered, when asked to
public class FiniteStateCreature  {
	
	Random rnd = new Random();
	private float speed = .1f;
	//monster image from https://omagerio.itch.io/1200-tiny-monsters-sprites-16x16
	public enum CreatureState {SEARCHING, EATING, DEAD}
	private CreatureState currentState;
	private Point.Float position, movement;
	private Image regularImage, chewImage, deadImage;
	private Dimension2D terrainBorder;
	private float directionInRadians;
	private ArrayList<Point.Float> foodPlacements;
	private long msLeftOfEating = 0;
	private float foodInBelly = 100;

	public CreatureState getCurrentState() {
		return currentState;
	}

	public void setCurrentState(CreatureState currentState) {
		this.currentState = currentState; 
		if(this.getCurrentState()==CreatureState.EATING) {
			msLeftOfEating = 2000;
		}
	}
	
	public int getWidth() {return getImage().getWidth(null);}
	public int getHeight() {return getImage().getHeight(null);}
	
	public Point.Float getPosition() {return position;}
	public void setPosition(Point.Float position) {this.position = position;}

	public Point.Float getMovement() {return movement;}
	public void setMovement(Point.Float movement) {this.movement = movement;}
	
	public Image getImage() {
		switch (getCurrentState()) {
		
		case SEARCHING: return regularImage;
		
		case DEAD : return deadImage;
		
		case EATING: 
			long millisecondPartOfCurrentTime = System.currentTimeMillis() % 1000;
			int quarterOfSecond = (int)millisecondPartOfCurrentTime / 250; 
			boolean quarterIsEvenNumber = quarterOfSecond % 2 == 0;
			if(quarterIsEvenNumber) return chewImage;
			else return regularImage;
		
		default: return null;
		}
	}

	public void setImage(Image image) {
		this.regularImage = image;
	}
	
	public Dimension2D getTerrainBorder() {
		return terrainBorder;
	}

	public void setTerrainBorder(Dimension2D terrainBorder) {
		this.terrainBorder = terrainBorder;
	}
	
	public float getFoodInBelly() {
		return foodInBelly;
	}

	public void setFoodInBelly(float foodInBelly) {
		this.foodInBelly = foodInBelly;
		if(this.getFoodInBelly() > 100) {
			setFoodInBelly(100);
		}
		else if (getFoodInBelly()<= 0) {
			setCurrentState(CreatureState.DEAD);
		}
	}	
	
	public FiniteStateCreature(Point.Float position, Point.Float movement, Image image, Image chewImage, Image deadImage, ArrayList<Point.Float> foodPlacements, CreatureState currenState, Dimension2D terrainBorder) {
		this.setPosition(position);
		this.setMovement(movement);
		this.regularImage = image;
		this.chewImage = chewImage;
		this.deadImage = deadImage;
		this.foodPlacements = foodPlacements;
		this.setCurrentState(currenState);
		this.setTerrainBorder(terrainBorder);
	}

	public void update(double msElapsedSinceLastUpdate) {
		
		switch (getCurrentState()) {
		case SEARCHING:
			wander(msElapsedSinceLastUpdate);	
			break;
		case EATING:
			eat(msElapsedSinceLastUpdate);
			break;
		default:
			break;
		}
	}

	private void eat(double msElapsedSinceLastUpdate) {
		msLeftOfEating -= msElapsedSinceLastUpdate;
		if(msLeftOfEating <= 0) {
			this.setFoodInBelly(getFoodInBelly()+30);
			setCurrentState(CreatureState.SEARCHING);
		}
	}

	private void wander(double msElapsedSinceLastUpdate) {
		turnSlightlyRightOrLeftAtRandom();
		Point.Float newPosition = new Point.Float(getPosition().x + getMovement().x * (float)msElapsedSinceLastUpdate, getPosition().y + getMovement().y* (float)msElapsedSinceLastUpdate);
		if(!touchesBorderAtPosition(newPosition)) {
			this.setPosition(newPosition);
		}
		else{
			findNewRandomDirection();
		}
		eatFoodIfCloseEnough();
		this.setFoodInBelly((float) (getFoodInBelly()-0.005f * msElapsedSinceLastUpdate));
	}

	private boolean touchesBorderAtPosition(java.awt.geom.Point2D.Float newPosition) {
		return newPosition.x - getWidth()/2 < 0 || newPosition.x + getWidth()/2 > getTerrainBorder().getWidth()  ||
				newPosition.y - getHeight()/2 < 0 || newPosition.y + getHeight()/2 > getTerrainBorder().getHeight();
		}

	private void eatFoodIfCloseEnough() {
		Point.Float closestFoodPlacement = getClosestFoodPlacement();
		if(closestFoodPlacement != null) {
			Point.Float position = getPosition();
			double distanceToClosestFood = closestFoodPlacement.distance(position);
			//System.out.println(distanceToClosestFood);
			if(distanceToClosestFood < this.getWidth()/2) {
				this.setCurrentState(FiniteStateCreature.CreatureState.EATING);
				foodPlacements.remove(closestFoodPlacement);
			}
		}
	}
	
	public void draw(Graphics g){				
		g.drawImage(getImage(), (int)getPosition().x - getWidth()/2, (int)getPosition().y- getHeight()/2, null);
		g.setColor(Color.white);
		g.setFont(FiniteStateMachinePanel.FONT);
		g.drawString(getCurrentState().toString(), (int)getPosition().x - 45, (int)getPosition().y-20);
		drawLifebar(g);
	}
	
	private void drawLifebar(Graphics g) {
		int width = 30;
		int height = 7;
		int left = (int)getPosition().x - width/2;
		int top = (int)getPosition().y + 20;
		g.setColor(Color.black);
		g.fillRect(left, top, width, height);
		setColorBasedOnBellyFullness(g);
		g.fillRect(left, top, (int) (width * getFoodInBelly()/100), height);
		g.setColor(Color.black);
		g.drawRect(left, top, width, height);
		
	}

	private void setColorBasedOnBellyFullness(Graphics g) {
		if(this.getFoodInBelly()> 70) {g.setColor(Color.green);}
		else if(this.getFoodInBelly() > 50) {g.setColor(Color.yellow);}
		else if(this.getFoodInBelly() > 30) {g.setColor(Color.orange);}
		else {g.setColor(Color.red);}
	}

	private void findNewRandomDirection() {
		float randomDirectionInRadians = rnd.nextFloat() * (float)Math.PI * 2;
		this.setDirectionInRadians(randomDirectionInRadians);
	}
	
	private void turnSlightlyRightOrLeftAtRandom() {
		float randomDirectionChangeInRadians = (rnd.nextFloat() * (float)Math.PI / 8) -  (float)Math.PI / 16;
		this.setDirectionInRadians(this.getDirectionInRadians()+randomDirectionChangeInRadians);
	}

	public float getDirectionInRadians() {
		return directionInRadians;
	}

	public void setDirectionInRadians(float directionInRadians) {
		this.directionInRadians = directionInRadians;
		this.getMovement().x = (float)Math.cos(getDirectionInRadians())*speed;
		this.getMovement().y = (float)Math.sin(getDirectionInRadians())*speed;
	}
	
	private Point.Float getClosestFoodPlacement(){
		
		double smallestDistanceSoFar = Float.MAX_VALUE;
		Point.Float closestFoodPlacementSoFar = null;
		
		for (int i = 0; i < foodPlacements.size(); i++) {
			Point.Float currentPlacement = foodPlacements.get(i);
			double distanceToCurrentPlacement = currentPlacement.distance(getPosition());
			if(distanceToCurrentPlacement < smallestDistanceSoFar) {
				closestFoodPlacementSoFar = currentPlacement;
				smallestDistanceSoFar = distanceToCurrentPlacement;
			}
		}
		return closestFoodPlacementSoFar;
	}
}